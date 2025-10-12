---
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git branch:*), Bash(git log:*), Bash(gh pr create:*)
argument-hint: [issue-number]
description: Create PR following project template
---

# Create Pull Request

@agent-github-manager

Analyze current branch changes and create a Pull Request linked to issue #$ARGUMENTS.

## Context

- Current branch: !`git branch --show-current`
- Git status: !`git status`
- Changes: !`git diff HEAD`
- Commit history: !`git log --oneline -10`
- Diff from main: !`git diff main...HEAD`

Follow project PR template and conventions. Respond and work in Korean.