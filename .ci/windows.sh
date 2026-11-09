#!/bin/sh -ex

mkdir -p build
cd build

EXTRA_CMAKE_FLAGS=()

if [ "$GITHUB_REF_TYPE" = "tag" ]; then
	EXTRA_CMAKE_FLAGS=(-DENABLE_QT_UPDATE_CHECKER=ON)
fi

if [ "$TARGET" = "msvc" ]; then
	# MSVC builds get FFmpeg from vcpkg, which the workflow installs beforehand.
	EXTRA_CMAKE_FLAGS=("${EXTRA_CMAKE_FLAGS[@]}"
		-DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake
		-DVCPKG_TARGET_TRIPLET=x64-windows-release)
fi

cmake .. -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_COMPILER_LAUNCHER=ccache \
    -DCMAKE_CXX_COMPILER_LAUNCHER=ccache \
    -DENABLE_QT_TRANSLATION=ON \
    -DUSE_DISCORD_PRESENCE=ON \
	"${EXTRA_CMAKE_FLAGS[@]}"
ninja
ninja bundle
strip -s bundle/*.exe

ccache -s -v

ctest -VV -C Release || echo "::error ::Test error occurred on Windows build"
