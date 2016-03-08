# 2. move waiting code into a seperate concern

Date: 08/03/2016

## Status

Accepted

## Context

We started with waiting code spread throughout the different layers of the DockerComposition stack:
 * DockerComposition
 * Container
 * DockerPort

This makes our custom waits depend on specific functionality rather than general functionality that external devs can also use. Now we're making all `waitForService` calls go through just a couple of API calls with flexible Healthchecks this is going to bite us.

## Decision

Wait code (for the purposes of powering `waitForService`) does not go in the Composition stack. Instead, waits depend on external observations of this stack.

## Consequences

The composition stack only talks about it's current state at any given point
