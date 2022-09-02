# clj-qp - Quadratic Programming / Optimization in Clojure

Solve convex [quadratic programs](https://en.wikipedia.org/wiki/Quadratic_programming) (QP) in clojure.

This library is a wrapper around the Java API of the [FICO Xpress Solver](https://www.fico.com/fico-xpress-optimization/docs/latest/overview.html).

## Installation

Lastest release is 0.5, available at Clojars.

CLI/deps.edn dependency information:
```
org.clojars.philipperolet/clj-qp {:mvn/version "0.5"}
```

Leiningen:
```
[org.clojars.philipperolet/clj-qp "0.5"]
```

### Requirements
FICO Xpress Optimization **must** be installed for the lib to work, since the library wraps its API. There is a free community version available.

- Download the [FICO Xpress Optimization package](https://content.fico.com/xpress-optimization-community-license?utm_source=FICO-Community&utm_medium=optimization-homepage)
- Install FICO Xpress Optimization: [Installation instructions](https://www.fico.com/fico-xpress-optimization/docs/latest/installguide/dhtml/chapinst1.html)
  - Linux: questions when running the install.sh script: if unsure, answer [c] for community edition, then [n] for Xpress-Kalis and [y] to add to .bashrc
  - Ensure the env vars are set properly
	- this is done automatically after a terminal restart if you use Bash and answered y at the last question mentionned above

## Usage

Two main functions are available:

- ``(solve-qp ...) `` solves a quadratic program;
- ``(solve-lcls ...)`` solves a linearly constrained least squares problem.

Details can be found on the [API Documentation](https://www.machine-zero.com/clj-qp/api.html)

### Extending the library to other optimization programs (linear, etc.)
The library wraps specific parts of the FICO Xpress Optimization API dedicated to particular forms of quadratic programs. 

For a clojure developer, it should be straightforward to extend it to wrap other API calls (e.g. solving linear programs or quadratically constrained programs) by using `solve-qp` in `xprs.clj` as a template. The FICO API & documentation is available [here](https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/GUID-3BEAAE64-B07F-302C-B880-A11C2C4AF4F6.html).

### Usage limitations
Please note that the FICO optimization software wrapped by `clj-qp` has limitations depending on the license chosen at the time of installation. The community version is free but has quotas for quadratic program sizes and other usage limitations. Please checks if your intended usage fits the license limitations.

## Important disclaimers
- This library and its authors are not affiliated in any way with FICO and the Xpress Optimization Software
- This library comes with no warranty or liability as stated in the license
- The library has been tested for simple uses and research purposes, but has not been tested for production environments
