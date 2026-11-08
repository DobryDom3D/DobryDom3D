# Research — Nishita sky model as an additional sky background

Status: research only, nothing implemented. Written as an enhancement-ticket body.
Paths: `LY/` = `../libYafaRay`, `SF/` = `../sunflow`, unprefixed = this repo.
No web access was available while writing. Statements about the papers come from
the standard published descriptions, are not re-checked against the PDFs, and are
marked "(unverified)". Everything about the code was read from source.

## Motivation

The only skies today are Preetham-family (`sunsky`, `SunSky2`/darksky). They give
a plausible clear day but have no physical atmosphere: no aerial perspective, no
observer-altitude dependence, no controllable ground albedo, and no twilight
below the horizon beyond a hack. A physically based sky (Nishita single scatter,
then multiple scattering) would give better dawn/dusk colours and, with the
sun/sky pair coming from one model, a consistent illumination of interiors.

## Findings

### Preetham limits

- Preetham is a closed-form Perez distribution fitted to simulated data, with
  turbidity as the only atmospheric input (`LY/src/backgrounds/sunsky.cc:110`
  `PerezFunction`, `:143` `getSkyCol`). There is no scattering integral, so
  multi-scattering, ground albedo, or altitude cannot be added to it; a new
  model must be a new background, not an extension.
- `sunsky.cc` and `darksky.cc` both hard-code a Z-up world and derive sun colour
  from `ComputeAttenuatedSunlight(theta, turbidity)` (`sunsky.cc:264`).

### Q1. Is the background light tied to sunsky? No, it is generic

- `background_t` (`LY/include/core_api/background.h:12-28`) exposes only
  `operator()(ray,state)`, `eval(ray)`, `hasIBL()`, `shootsCaustic()`.
- `bgLight_t` (`LY/src/lights/bglight.cc`) only calls `background->eval(ray, true)`
  (`:108` table build, `:197`, `:214`, `:236`, `:258`). It knows nothing about
  sunsky. `hasIBL()` is consumed only by the path tracer and photon integrator
  for caustic photons (`pathtracer.cc:259`, `photonintegr.cc:1233`); the light
  itself is created by the background's own factory, not by the core.
- Caveat: the factory is responsible for creating the light. `sunskyBackground_t::factory`
  builds `bgp["type"]="bglight"`, calls `render.createLight("sunsky_bgLight")`,
  `bglight->setBackground(bg)`, and `addLight` (`sunsky.cc:246-258`). A new
  background must copy that block (or share a helper). Note the pre-existing
  null-check-after-use at `sunsky.cc:255-257`.
- Importance sampling: at `bgLight_t::init` (`bglight.cc:75-135`) the sky is
  tabulated once on a latitude/longitude grid, `MAX_VSAMPLES=360` rows and
  16..`MAX_USAMPLES=720` columns per row (`:32-34`), weighted by sin(theta), into
  a marginal/conditional `pdf1D_t` pair. That is roughly 1.6e5 `eval()` calls per
  render setup (estimate from the constants: 360 rows, mean about 16+0.64*704
  columns). Sampling (`CalcFromSample`, `:138-150`) and pdf (`CalcFromDir`, `:152`)
  use only the tables. `illumSample` then does one `eval()` per light sample (`:197`).
- Cost conclusion: a per-sample ray-marched sky is NOT needed and would be the
  wrong design. The table build is a one-off about 1.6e5 evaluations, tolerable
  even for a naive single-scatter march (a few ms to low seconds, unmeasured).
  Per-sample `eval()` plus camera-ray `operator()` (`pathtracer.cc:328`) are the
  hot calls and run once per ray or light sample, so they must be O(1): either
  precompute a sky-view LUT (Hillaire) or a lat/long radiance texture at
  construction and look up. A naive march per camera pixel sample at 12 light
  samples per hit would be too slow; a lookup is not.
- Note the grid assumes `eval()` is direction-only (ray origin ignored; `bglight.cc:223`
  uses origin 0,0,0). An aerial-perspective or altitude-dependent sky would not fit
  without fixing the observer position, which is acceptable for a sky dome.

### Q2. Registration and the Java to factory path

- Each background is a separate shared plugin: `LY/src/backgrounds/CMakeLists.txt`
  (`add_library(sunsky ...)`, `SunSky2`, `textureback`, `gradientback`, one
  `install(TARGETS ...)` line). The plugin exports `registerPlugin(render)` which
  calls `render.registerFactory("sunsky", factory)` (`sunsky.cc:293-295`).
- `renderEnvironment_t::loadPlugins` dlopens every library in the plugin dir and
  calls `registerPlugin` (`LY/src/yafraycore/environment.cc:136-149`); the
  `background_factory_t` overload stores it in a map (`:1111-1115`).
- `createBackground(name, params)` reads `"type"`, looks up the factory, and stores
  the result under `name`; unknown type is an error that returns null (`:386-410`).
- JNI: `YafarayRenderer.createBackground` (Java `:1773`) maps to
  `Java_..._createBackground` (`src/.../j3d/YafarayRenderer.cpp:319`), which
  converts the `Map<String,Object>` with `getParams` (`:93-160`). Supported value
  types: String, Integer, Boolean, Float, Double, `float[]` (3/4 elements,
  point/colour). So a new sky needs no JNI change, only new keys. A `float[]`
  of larger size or arrays of arrays is not supported.
- A new plugin means a new CMake target plus packaging of the extra
  library everywhere the existing ones ship (check installer and CI scripts).
  Alternative: add the factory to an existing plugin to avoid packaging changes.

### Q3. Sun colour and the consistency issue

- `YafarayRenderer.java:595-608`: the sun direction comes from the compass/time;
  a Sunflow `SunSkyLight` is instantiated only to read `getSunColor()`
  (`SF/src/org/sunflow/core/light/SunSkyLight.java:324`, which is
  `getSkyRGB(sunDirWorld).constrainRGB()` of the Preetham sky itself, not a
  transmittance). The code comment says YafaRay's `ComputeAttenuatedSunlight`
  is "not accessible" from Java.
- That colour feeds (a) the faraway `spherelight` "sun" at `:621-637`, tinted by
  `sqrt(sunColor)`, and (b) the ambient-occlusion complementary colour at
  `:660-662`. Power factors `1.5f*2^photoSunPower` (sky) and 20 or 40 (sun,
  `:621`) are hand-tuned against the Preetham sky level.
- Consistency issue: with a Nishita sky the visible sky and the sun disc would
  come from two models. The sun tint would be reddish/white per Preetham while the
  sky horizon is per Nishita, and the sun/sky energy ratio tuned in `:609-621`
  would shift. Proper fix: the new background computes sun transmittance
  `exp(-tau)` along the sun ray itself (the same optical depth the sky march
  already integrates) and exposes it. Since JNI only passes parameters in and
  cannot read back, the options are: (a) the new factory creates the sun light
  itself, as `sunsky` does with `add_sun` (`sunsky.cc:262-283`), using a `sunlight`
  type with its own transmittance (preferred); or (b) port the sun-transmittance
  maths to Java for the Java-side `spherelight`. Option (a) removes the
  `SunSkyLight` dependency for the new sky and fixes the AO tint too if the
  tint is computed on the Java side from the same function (needs (b) for that
  part only).
- Hillaire's paper treats the sun as an analytic disc attenuated by a
  transmittance LUT (unverified detail; confirm in the paper).

### Q4. Saved file and version

- `Home.CURRENT_VERSION = 7401` (`src/.../model/Home.java:53`, doc comment `:45-52`),
  deliberately above Sweet Home 3D 7.5's 7400 so upstream warns on DobryDom3D files.
  The comment says to increase it when a serialised field changes.
- `HomeEnvironment` is Java-serialised (`turbidity` field at `HomeEnvironment.java:78`,
  default set in two constructors `:163`, `:182`, getter/setter `:479-492`, fires a
  `Property` event). Adding an enum/int field means: a new `Property` constant,
  field defaults in both constructors, and a `readObject` default for old files
  (an absent field in old serialised homes reads as 0/null, so make 0 equal
  "Preetham"). Bump `CURRENT_VERSION` to 7402 per the comment (policy decision, see open
  questions).
- XML: `HomeXMLExporter.java:191` writes `writeFloatAttribute("turbidity", ...)` and
  `HomeXMLHandler.java:988-990` reads it with `parseOptionalFloat`; the DTD-style
  comment at `HomeXMLHandler.java:~108` lists attributes. A new optional attribute
  (e.g. `skyModel`) read with an optional parser leaves old files working; an
  old reader (including upstream Sweet Home 3D) ignores unknown attributes
  (unverified for upstream's SAX handler, check) and falls back to Preetham.
- UI: `PhotoPanel.java:143,490-504` has turbidity label/spinner wired to
  `AbstractPhotoController.Property.TURBIDITY`; a sky-model combo follows that
  pattern (controller property, panel combo, localised labels in the bundles).
  Turbidity maps naturally to Rayleigh/Mie density, but a Nishita model usually
  takes aerosol density and ground albedo; decide whether to reuse turbidity.

### Q5. Sunflow path

- `PhotoRenderer.java:534-545` builds the sky with Sunflow's built-in `"sunsky"`
  light (Preetham, Sunflow's own implementation), and `:548-557` reads the sun
  colour the same way as the YafaRay path.
- Sunflow has no pluggable sky background in this code path; adding Nishita there
  means a new `LightSource` class in `SF/` (sunflow is a sibling repo). Cheaper
  option: expose the new sky only for the YafaRay renderer and make the combo
  disabled (or silently Preetham) when the Sunflow renderer is selected, with the
  choice documented. Sunflow is the older renderer, so this limitation is cheap to accept.
  Either way `PhotoRenderer` must not receive an unknown sky model value and crash.
- Both renderers share the `useSunSky`/`useSunskyLight` logic (`YafarayRenderer.java:308,367`,
  `PhotoRenderer.java:223,274`): sky texture plus an observer camera replaces the sky
  with an IBL texture. A new model should obey the same rule.

### Algorithms (what the sources say)

- Nishita et al. 1993, "Display of the Earth Taking into Account Atmospheric
  Scattering" (SIGGRAPH '93): single scattering of Rayleigh and Mie in a
  spherical atmosphere, optical depths by line integration along view and
  sun rays; the paper's own efficiency idea is precomputing/tabulating optical
  depth (details unverified, check paper). Rayleigh and Mie scale heights are commonly
  quoted as about 8 km and 1.2 km (unverified; take from the paper's table).
- Bruneton and Neyret 2008, "Precomputed Atmospheric Scattering" (EGSR): precomputes
  transmittance and (multi-order) scattering into 2D/4D LUTs; the author's page
  `ebruneton.github.io/precomputed_atmospheric_scattering` provides a reference
  implementation (licence needs checking before reuse; unverified).
- Hillaire 2020, "A Scalable and Production Ready Sky and Atmosphere Rendering
  Technique" (EGSR / CGF): transmittance LUT, a small multi-scattering LUT that
  approximates higher orders with an isotropic-phase geometric-series assumption,
  a sky-view LUT, and aerial perspective volume (author's page
  `sebh.github.io/publications/egsr2020.pdf` and the reference code on GitHub
  `sebh/UnrealEngineSkyAtmosphere`, MIT licence, unverified, check). Only the
  transmittance + multiple-scattering + sky-view parts are relevant; the aerial
  perspective volume is not needed for a sky dome.

## Proposal

Phase 0 (shared plumbing, no new physics): `HomeEnvironment` sky-model property
(Preetham default), XML attribute, controller/PhotoPanel combo, Sunflow fallback,
version bump. Land with only "Preetham" selectable to prove the round trip.

Phase 1 (single scatter Nishita): new YafaRay background factory `"nishita"`
(own file or added to `sunsky.cc`'s plugin). At construction, build a
sky-view radiance LUT (azimuth relative to sun by elevation) by marching the
view ray with 16-32 primary steps and 8 light steps (Rayleigh+Mie, Cornette-
Shanks or Henyey-Greenstein for Mie), plus a transmittance table; `eval()` and
`operator()` become bilinear lookups. Add the sun disc via a factory-made `sunlight`
with the same transmittance. Reuse the `bglight` block from `sunsky.cc:246-258`.
Calibrate `power` so the default view matches the Preetham brightness (the
tuned `:609,621` constants).

Phase 2 (multi-scatter): add Hillaire's multi-scattering LUT and fold it into the
sky-view LUT. Validate against a brute-force multi-order reference (or Bruneton's
reference results) at several sun elevations; ground albedo becomes a parameter.

Validation per phase: render a daylight/sunset/twilight triplet for each model,
compare sky luminance vs elevation, and compare the interior illumination ratio
sun:sky for an aerial view and for an observer view.

## Touch points

| Area | File |
|---|---|
| New background plugin | `LY/src/backgrounds/` (new file), `CMakeLists.txt` |
| Param keys from Java | `src/.../j3d/YafarayRenderer.java:~585-640` |
| Sun colour/transmittance | `YafarayRenderer.java:595-608, 621-637, 660-662` |
| Sunflow fallback | `src/.../j3d/PhotoRenderer.java:534-557` |
| Model property | `src/.../model/HomeEnvironment.java` |
| File version | `src/.../model/Home.java:53` |
| XML | `src/.../io/HomeXMLHandler.java:988`, `HomeXMLExporter.java:191` |
| UI/controller | `src/.../swing/PhotoPanel.java:~490`, `AbstractPhotoController`, resource bundles |
| Packaging | installer/CI that lists plugin libraries (not checked) |

JNI itself (`YafarayRenderer.cpp:93-160, 319`) needs no change.

## Risks

- Sun and sky model mismatch (above) if the sun stays on `SunSkyLight`.
- Brightness calibration: hand-tuned powers assume Preetham levels; expect a
  tuning pass and a risk of overexposed interiors or darker dusk looks.
- `bgLight_t` precomputes on a fixed 360x720 grid; a narrow sun glow in the sky
  LUT may be under-sampled (same issue as today, but a physical sun disc is
  sharper). Keep the disc out of the sky radiance and treat it as the separate `sunlight`.
- Coordinate conventions: YafaRay is Z-up (the Java side swaps to `(x,-z,y)`,
  `:613`); Sunflow Y-up. Phase 1 must be tested against the compass north.
- Licensing for any copied reference code (Bruneton BSD-style, Hillaire MIT:
  both unverified); a fresh implementation from the papers avoids the question.
- Old readers: files with the new attribute opened in upstream Sweet Home 3D
  or older DobryDom3D (see the XML note above, unverified).
- Extra shared library: packaging for Windows, macOS and Linux installers.

## Open questions

- Reuse the turbidity spinner for aerosol density, or add new controls (albedo,
  air/dust density)? How many knobs belong in the photo panel?
- New plugin or add to the `sunsky` plugin (packaging versus tidiness)?
- Does the Sunflow renderer stay Preetham-only, or is a Sunflow Nishita light worth it?
- Bump `CURRENT_VERSION` to 7402 for the new serialised field (follows the
  existing doc comment) or rely only on XML optionality?
- Is sun transmittance computed in Java (to colour the AO ambient) or only in C++?
- Does the user want physically accurate night/twilight (below the horizon the
  code cuts the sun at `sunDirection[1] > -0.075f`, `YafarayRenderer.java:598`)?
