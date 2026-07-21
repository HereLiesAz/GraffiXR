# Migration of Chicory WASM Runtime to 1.7.5

The project recently updated Chicory to version 1.7.5, which contains significant breaking changes in the API. This plan outlines the necessary updates to `JsSandbox.kt` and `WasmSandbox.kt` to restore compilation.

## Proposed Changes

### core:data

#### [MODIFY] [JsSandbox.kt](file:///C:/Users/azrie/StudioProjects/Graffux/core/data/src/main/java/com/hereliesaz/graffitixr/data/azphalt/sandbox/JsSandbox.kt)
- Update imports for Chicory 1.7.5.
- Replace `com.dylibso.chicory.runtime.Module` with `com.dylibso.chicory.wasm.WasmModule` and use `Parser.parse()` to load WASM bytes.
- Replace `HostImports` with `ImportValues`.
- Update `WasiPreview1` instantiation to use the builder.
- Update `HostFunction` instantiation:
    - Use `ValType` instead of `ValueType`.
    - Use `FunctionType.of()` for parameter and return types.
    - Update lambda signature to `(Instance, LongArray) -> LongArray?`.
- Update `apply()` calls:
    - Pass `long` values instead of `Value` objects.
    - Handle `LongArray` return and use `toInt()` to extract results.
- Fix `Value.i32()` and `Value.asInt()` calls to match the new `Long`-based API.

#### [MODIFY] [WasmSandbox.kt](file:///C:/Users/azrie/StudioProjects/Graffux/core/data/src/main/java/com/hereliesaz/graffitixr/data/azphalt/sandbox/WasmSandbox.kt)
- Update imports for Chicory 1.7.5.
- Update `HostFunction` definitions to use the new signature and types.
- Update memory access to use the updated `Memory` interface methods.
- Update host function returns to return `LongArray?`.

## Verification Plan

### Automated Tests
- Run `:core:data:compileDebugKotlin` to verify the fix.
- Run tests in `:core:data` if any exist for sandboxing (e.g., `JsSandboxTest`).

### Manual Verification
- None required as this is a compilation fix.
