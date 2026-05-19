# RTP statechart tooling.
#
#   make viz     — render every *.statechart.json sidecar to dist/*.html
#   make build   — codegen TS files for the Stately VS Code extension
#   make canvas  — run the Luminous statechart-canvas pipeline (.luminous/)
#   make watch   — same as build, in chokidar watch mode
#   make clean   — drop generated artifacts
#
# `viz` / `canvas` have no dependencies beyond Python 3 / Node stdlib. `build` /
# `watch` require `npm install` once for xstate + chokidar.

PY ?= python3

.PHONY: viz build canvas watch clean help

help:
	@grep -E '^# +' Makefile | sed 's/^# *//'

viz:
	$(PY) -m statechart_viz

build:
	node build.mjs

canvas:
	node ../../.luminous/statechart-canvas.pipeline.mjs

watch:
	node build.mjs --watch

clean:
	rm -rf dist generated/*.machine.ts generated/index.ts
