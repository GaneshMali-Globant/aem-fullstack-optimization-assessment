import { mkdirSync, copyFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const sourceFile = resolve("src/main/frontend/src/index.js");
const outputFile = resolve("src/main/frontend/dist/index.js");

mkdirSync(dirname(outputFile), { recursive: true });
copyFileSync(sourceFile, outputFile);

