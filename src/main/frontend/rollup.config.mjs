import { nodeResolve } from "@rollup/plugin-node-resolve";
import terser from "@rollup/plugin-terser";

export default {
  input: "src/editor.js",
  output: {
    file: "../resources/public/javascript/editor.bundle.js",
    format: "iife",
    name: "RotomEditor",
    sourcemap: true,
    inlineDynamicImports: true,
  },
  plugins: [
    nodeResolve(),
    terser(),
  ],
};
