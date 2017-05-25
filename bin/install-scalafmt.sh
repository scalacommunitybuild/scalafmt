#!/bin/sh
set -u

main() {
    need_cmd wget
    need_cmd mktemp
    need_cmd chmod
    need_cmd mkdir
    need_cmd rm

    local _dir="$(mktemp -d 2>/dev/null)"
    local _coursier="$_dir/coursier"
    local _scalafmt="scalafmt"
    ensure mkdir -p "$_dir"
    ensure wget -q https://github.com/coursier/coursier/raw/master/coursier -O "$_coursier"
    ensure chmod u+x "$_coursier"
    ensure $_coursier bootstrap com.geirsson:scalafmt-cli_2.12:0.7.0-RC1 -o "$_scalafmt" --main org.scalafmt.cli.Cli -f
    ensure $_scalafmt --help
    ignore rm -rf "$_dir"
}

say() {
    echo "install-scalafmt: $1"
}

err() {
    say "$1" >&2
    exit 1
}

need_cmd() {
    if ! command -v "$1" > /dev/null 2>&1
    then err "need '$1' (command not found)"
    fi
}

need_ok() {
    if [ $? != 0 ]; then err "$1"; fi
}

ensure() {
    "$@"
    need_ok "command failed: $*"
}

ignore() {
    "$@"
}

main "$@" || exit 1
