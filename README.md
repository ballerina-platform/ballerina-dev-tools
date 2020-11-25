# Ballerina Dev Tools Library
 [![Build main](https://github.com/ballerina-platform/ballerina-dev-tools/workflows/Build%20master/badge.svg?branch=main)](https://github.com/ballerina-platform/ballerina-dev-tools/actions?query=workflow%3ABuild)
 [![Daily build](https://github.com/ballerina-platform/ballerina-dev-tools/workflows/Daily%20build/badge.svg)](https://github.com/ballerina-platform/ballerina-dev-tools/actions?query=workflow%3A%22Daily+build%22)
 [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/ballerina-dev-tools.svg)](https://github.com/ballerina-platform/ballerina-dev-tools/commits/master)

Ballerina Dev tools library contains all the npm builds required by the ballerina distribution.

## Building from the Source

### Setting Up the Prerequisites

1. Node.js (version 10.22.1)
2. npm (version 6.14.6 or later)

     
### Building the Source

Execute the commands below to build from the source.

1. To build the library:
        
        ./gradlew clean build

2. To publish to maven local:

        ./gradlew clean build publishToMavenLocal

## Contributing to Ballerina

As an open-source project, Ballerina welcomes contributions from the community. 

You can also check for [open issues](https://github.com/ballerina-platform/ballerina-dev-tools/issues) that
 interest you. We look forward to receiving your contributions.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Discuss about code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](performance/benchmarks/summary.md).
