## Basic guidelines

All packages you commit or submit by pull-request should follow these simple
guidelines:

- Package a version which is still maintained by the upstream author and will
  be updated regularly with supported versions.
- Have no dependencies outside the OpenWrt core packages or this repository
  feed.
- Have been tested to compile with the correct includes and dependencies.
  Please also test with "Compile with full language support" found under
  "General Build Settings" set if language support is relevant to your package.
- Best of all -- it works as expected!

## If you have commit access

- Do NOT use git push --force.
- Do NOT commit to other maintainer's packages without their consent.
- Use Pull Requests if you are unsure and to suggest changes to other
  maintainers.

### Gaining commit access

- We will gladly grant commit access to responsible contributors who have made
  useful pull requests and / or feedback or patches to this repository or
  OpenWrt in general. Please include your request for commit access in your next
  pull request or ticket.

## Release Branches

- Old stable branches were named after the following pattern "for-XX.YY" (e.g.
  for-14.07) before the LEDE split.  During the LEDE split there was only one
  release branch with the name "lede-17.01".  After merging the LEDE fork with
  OpenWrt the release branches are named according to the following pattern
  "openwrt-XX.YY" (e.g. openwrt-18.06).
- These branches are built with the respective OpenWrt release and are created
  during the release stabilisation phase.
- Please ONLY cherry-pick or commit security and bug-fixes to these branches.
- Do NOT add new packages and do NOT do major upgrades of packages here.
- If you are unsure if your change is suitable, please use a pull request.

## Common LICENSE tags (short list)

(Complete list can be found at: <https://spdx.org/licenses>)

| Full Name                                        | Identifier               |
| ------------------------------------------------ | :----------------------- |
| Apache License 1.0                               | Apache-1.0               |
| Apache License 1.1                               | Apache-1.1               |
| Apache License 2.0                               | Apache-2.0               |
| Artistic License 1.0                             | Artistic-1.0             |
| Artistic License 1.0 w/clause 8                  | Artistic-1.0-cl8         |
| Artistic License 1.0 (Perl)                      | Artistic-1.0-Perl        |
| Artistic License 2.0                             | Artistic-2.0             |
| BSD 2-Clause "Simplified" License                | BSD-2-Clause             |
| BSD 2-Clause FreeBSD License                     | BSD-2-Clause-FreeBSD     |
| BSD 2-Clause NetBSD License                      | BSD-2-Clause-NetBSD      |
| BSD 3-Clause "New" or "Revised" License          | BSD-3-Clause             |
| BSD with attribution                             | BSD-3-Clause-Attribution |
| BSD 3-Clause Clear License                       | BSD-3-Clause-Clear       |
| BSD 4-Clause "Original" or "Old" License         | BSD-4-Clause             |
| BSD-4-Clause (University of California-Specific) | BSD-4-Clause-UC          |
| BSD Protection License                           | BSD-Protection           |
| GNU General Public License v1.0 only             | GPL-1.0-only             |
| GNU General Public License v1.0 or later         | GPL-1.0-or-later         |
| GNU General Public License v2.0 only             | GPL-2.0-only             |
| GNU General Public License v2.0 or later         | GPL-2.0-or-later         |
| GNU General Public License v3.0 only             | GPL-3.0-only             |
| GNU General Public License v3.0 or later         | GPL-3.0-or-later         |
| GNU Lesser General Public License v2.1 only      | LGPL-2.1-only            |
| GNU Lesser General Public License v2.1 or later  | LGPL-2.1-or-later        |
| GNU Lesser General Public License v3.0 only      | LGPL-3.0-only            |
| GNU Lesser General Public License v3.0 or later  | LGPL-3.0-or-later        |
| GNU Library General Public License v2 only       | LGPL-2.0-only            |
| GNU Library General Public License v2 or later   | LGPL-2.0-or-later        |
| Fair License                                     | Fair                     |
| ISC License                                      | ISC                      |
| MIT License                                      | MIT                      |
| No Limit Public License                          | NLPL                     |
| OpenSSL License                                  | OpenSSL                  |
| X11 License                                      | X11                      |
| zlib License                                     | Zlib                     |
