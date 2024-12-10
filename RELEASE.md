# Release

Releases in this repository are managed using [release-please](https://github.com/googleapis/release-please), an
automated GitHub Action that updates project versions and generates release notes based on commit messages. With Maven
configured in the project, `release-please` identifies version changes, updates `pom.xml`, and ensures new versions are
pushed to GitHub Packages.

## How It Works

1. **Commit-based Version Bumps**: Commit messages trigger version increments (`feat:`, `fix:`, etc.) based
   on [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
2. **Release Creation**: `release-please` runs to create a new release and drafts the corresponding release notes.
3. **Maven Deployment**: A second job builds the project and deploys artifacts to GitHub Packages using Maven.

For detailed configuration and usage, refer to
the [release-please Java and Maven Strategies](https://github.com/googleapis/release-please/blob/main/docs/java.md).
