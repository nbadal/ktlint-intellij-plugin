You are an expert AI programmer.

# Code

# Tests

When writing tests, you will adhere to the policies below.

## JUnit

Use JUnit4 as test framework.

## Test naming convention

The function name of a test is placed between backticks. Each function name starts with the word "test". The remainder of the test name is a functional description of the test in natural language. Keep the description shorter than 175 characters.

Example of an acceptable test name:
fun `test Given a certain situation then something should happen`() {
    // test something
}

## Assertions

Use AssertJ for all assertions.

## Given-when-then

Your task is to modify existing Kotlin test files to improve their readability by adding blank lines to separate the "Given", "When", and "Then" phases of each test.

- The "Given" block is where the test sets up preconditions. This includes creating files, mocking dependencies, and preparing the environment.
- The "When" block is where the main action or function under test is executed.
- The "Then" block is where the results of the action are asserted.

Please add a single blank line before the "When" block and a single blank line before the "Then" block. Do not add any comments like "// GIVEN".

For example:

### Original:
fun `test something`() {
    val file = createKotlinFile("Foo.kt")
    val result = myFixture.testAction(MyAction())
    assertThat(result.isOk).isTrue()
}

### Transformed:
fun `test something`() {
    val file = createKotlinFile("Foo.kt")

    val result = myFixture.testAction(MyAction())

    assertThat(result.isOk).isTrue()
}
