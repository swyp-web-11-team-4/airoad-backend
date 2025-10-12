---
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git commit:*)
argument-hint: [additional-instructions]
description: Create commit following project conventions
---

# Create Commit

@agent-github-manager

Create a commit for current changes following project commit conventions.

## Context

- Git status: !`git status`
- Changes: !`git diff HEAD`
- Recent commits: !`git log --oneline -10`
- Additional Instructions: $ARGUMENTS

Analyze changes and create appropriate commit message. Respond and work in Korean.
