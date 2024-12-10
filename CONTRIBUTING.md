# Contributing

We appreciate all kinds of contributions. The following is a set of guidelines for contributing to this repository on
GitHub. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this
document in a pull request.

By submitting a contribution to this repository you agree that you do this under the [license](LICENSE) of the
repository and certify that you have all the rights to do so.

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code.

## Found an Issue?

If you find a bug in the source code or a mistake in the documentation, you can help us by submitting an issue to
our [GitHub Repository](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter). Including an issue
reproduction is the absolute best way to help the team quickly diagnose the problem. Screenshots are also helpful.

You can help the team even more and Pull Request with a fix.

## Want a Feature?

You can *request* a new feature by submitting an issue to
our [GitHub Repository](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter). If you would like to
*implement* a new feature, please submit an issue with a proposal for your work first, to be sure that we can use it.

Please consider what kind of change it is:

* For a **Major Feature**, first open an issue and outline your proposal so that it can be
  discussed. This will also allow us to better coordinate our efforts, prevent duplication of work,
  and help you to craft the change so that it is successfully accepted into the project.
* **Small Features** can be crafted and directly submitted as a Pull Request.

## Submitting a Pull Request (PR)

Before you submit your Pull Request (PR) consider the following guidelines:

* Checkout a new branch: `feature/xxx` or `bugfix/xxx`
* Create your feature or patch, **including appropriate test cases**.
* Follow our [Coding Standards](CODING_STANDARDS.md).
* Run tests and ensure that all tests pass.
* Commit your changes using a descriptive commit message that follows our
  [commit message conventions](#commit).
* Push your branch to GitHub.

* In GitHub, send a pull request to `sbb-your-project:main`.
  The PR title and message should as well conform to the [commit message conventions](#commit).

## Commit Message Guidelines

This project uses [Conventional Commits](https://www.conventionalcommits.org/) to generate the changelog.

### Commit Message Format

```
<type>(<optional scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

Any line of the commit message cannot be longer 100 characters! This allows the message to be easier
to read on GitHub as well as in various git tools.

### Type

Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semicolons, ...)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests or correcting existing tests
* **build**: Changes that affect the build system, CI configuration or external dependencies (example scopes:
  maven, ci, ...)
* **chore**: Other changes that don't modify `src` or `test` files

### Scope

The scope could be anything specifying place of the commit change. For example `converter`, `gtfs`, etc.

### Subject

The subject contains succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize first letter
* no dot (.) at the end

### Body

Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behavior.

### Footer

The footer should contain any information about **Breaking Changes** and is also the place to
reference GitHub issues that this commit **Closes**.

**Breaking Changes** should start with the word `BREAKING CHANGE:` with a space or two newlines.
The rest of the commit message is then used for this.

## Attribution

This CONTRIBUTING guideline is adapted from
the [sbb-design-systems/sbb-angular](https://github.com/sbb-design-systems/sbb-angular)
