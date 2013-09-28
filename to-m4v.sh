#!/bin/zsh

foreach f ( *.h264 ); do ffmpeg -f h264 -an -i "$f" "$f.m4v"; done

