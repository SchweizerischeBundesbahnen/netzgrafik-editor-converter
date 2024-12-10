# Coding Standards

This repository uses the IntelliJ formatter with customizations defined in the [codeStyles](.idea/codeStyles)
configuration. Ensure that you format the project before committing changes.

- Add spaces after class definitions and before return statements if a method has more than three statements.
- Use spaces to group related blocks together (e.g., before `if` control flow statements).
- Follow these naming conventions:
    - Use `camelCase` for methods.
    - Use `UpperCamelCase` for classes.
    - Use `ALL_UPPER_CASE` for static constants.

Minimize boilerplate code by using Lombok to generate getters, setters, constructors, builders, or value classes.

Do not use `Optional<>` for parameters. It is permitted for return types and internal object usage. Use method
overloading and omit the parameter instead.

Example:

```java

@RequiredArgsConstructor
public class ExampleService {

    private static final int MAX_ATTEMPTS = 5;

    @Getter
    private final int reqeustCount = 0;

    private final ExampleRepository repository;

    public Optional<Example> findById(String id) {
        return repository.findById(id);
    }
}
```

## Design

- Restrict visibility as much as possible. Allow access from outside the package only when absolutely necessary.
- Program to interfaces, not implementations.
- Follow SOLID principles to structure your code (classes).
- Adhere to the DRY (Don't Repeat Yourself) principle.
- Avoid magic numbers or literals; use constants instead.

## Documentation

- Document only non-obvious public members using Javadoc (e.g., do not document simple getters or setters).
- Avoid comments in code except for complex logic or case structures. When comments are necessary, ensure they are clear
  and concise.

## Testing

- Use JUnit 5 for testing. The use of AssertJ and Mockito is permitted.
- The Surefire plugin runs unit tests (`ExampleTest`), while the Maven Failsafe plugin runs integration tests (
  `ExampleIT`).
- Follow the naming conventions with the `Test` and `IT` postfixes. Use descriptive names for test cases:
    - Use `should...`, `shouldNot...`, or `shouldThrow...`.
    - The `_` character is allowed in test case names to differentiate cases.
- Use nested test classes to group thematically related test cases.
- For complex test setups, use the test builder pattern or introduce a test extension for reusability.
