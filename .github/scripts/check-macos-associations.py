#!/usr/bin/env python3
# This file is part of DobryDom3D, a fork of Sweet Home 3D 7.5, modified by DobryDom3D contributors since August 2026.
"""Checks the file associations declared in the Info.plist of the macOS app.

DobryDom3D must be the owner of .dd3d and .dd3x files and only an alternate
handler of the .sh3d and .sh3x files written by Sweet Home 3D.

Usage: check-macos-associations.py path/to/Info.plist
"""
import plistlib
import sys

EXPECTED_RANKS = {
    "dd3d": "Owner",
    "dd3x": "Owner",
    "sh3d": "Alternate",
    "sh3x": "Alternate",
}


def extension_ranks(info):
    """Returns the LSHandlerRank declared for each file extension."""
    extensions_by_type = {}
    for declaration in info.get("UTExportedTypeDeclarations", []):
        tags = declaration.get("UTTypeTagSpecification", {})
        extensions_by_type[declaration["UTTypeIdentifier"]] = tags.get("public.filename-extension", [])
    ranks = {}
    for document_type in info.get("CFBundleDocumentTypes", []):
        extensions = list(document_type.get("CFBundleTypeExtensions", []))
        for content_type in document_type.get("LSItemContentTypes", []):
            extensions += extensions_by_type.get(content_type, [])
        for extension in extensions:
            ranks[extension.lower()] = document_type.get("LSHandlerRank")
    return ranks


def main(path):
    with open(path, "rb") as plist_file:
        ranks = extension_ranks(plistlib.load(plist_file))
    print("Declared handler ranks:", dict(sorted(ranks.items())))
    errors = []
    for extension, expected in EXPECTED_RANKS.items():
        if ranks.get(extension) != expected:
            errors.append("." + extension + ": expected rank " + expected + ", found " + str(ranks.get(extension)))
    for error in errors:
        print("::error::" + error)
    return 1 if errors else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1]))
