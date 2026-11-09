#!/bin/bash -ex

if grep -nrI '\s$' src *.yml *.txt *.md Doxyfile .gitignore .gitmodules .ci* dist/*.desktop \
                 dist/*.svg dist/*.xml; then
    echo Trailing whitespace found, aborting
    exit 1
fi

CLANG_FORMAT=clang-format
$CLANG_FORMAT --version

if [ -n "$COMMIT_RANGE" ]; then
    # Only check files changed by this push/pull request. This fork carries a lot of
    # pre-existing formatting drift, so linting every source file on each push drowns
    # real regressions in noise.
    files_to_lint="$(git diff --name-only --diff-filter=ACMRTUXB $COMMIT_RANGE | grep '^src/[^.]*[.]\(cpp\|h\)$' || true)"
else
    # Fall back to checking everything.
    files_to_lint="$(find src/ -name '*.cpp' -or -name '*.h')"
fi

# Turn off tracing for this because it's too verbose
set +x

for f in $files_to_lint; do
    d=$(diff -u "$f" <($CLANG_FORMAT "$f") || true)
    if ! [ -z "$d" ]; then
        echo "!!! $f not compliant to coding style, here is the fix:"
        echo "$d"
        fail=1
    fi
done

set -x

if [ "$fail" = 1 ]; then
    exit 1
fi
