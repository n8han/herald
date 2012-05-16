herald
======

herald is a standalone application that automates publishing release
notes to any Tumblog, but especially [notes.implicit.ly]. It
transforms notes from Markdown to HTML using [Knockoff], and posts
them using [Dispatch].

Install
-------

Install herald using [conscript][cs]:

    cs n8han/herald

[cs]: https://github.com/n8han/conscript#readme

Notes Specification
-------------------

Release notes for your project are expected under a `notes/` directory
in the root of the project. In this directory, notes for a particular
version are named `<version>.markdown` while an optional short
description about the project can be placed in `about.markdown`. For
example, in herald's tree you'll find the following:

    herald/
      notes/
        0.1.0.markdown
        0.1.1.markdown
        about.markdown

When publishing and previewing, the description from `about.markdown`
is appended to the selected version's release notes as boilerplate.

If you're publishing to the Scala software announcement site
[notes.implicit.ly], please keep in mind that the post's title will be
the name of your project and its corresponding version: these should
not be repeated as a heading in the notes. It is best to lead with
copy describing the changes in your release, or a list of those
changes. For major releases with changes divided into sections, use an
`h3` (a line prefixed by `###` in Markdown) or smaller heading. The
short description `about.markdown` should be one or two sentences
long, with a link to more information about your project.

Preview
-------

You can preview your release notes for any project by running `herald`
in its base directory. Herald assumes the notes you want to work with
are the highest numbered ones in your `notes` directory.

Publication Target Site
-----------------------

This plugin comes preconfigured to publish to [notes.implicit.ly]. To
be added as a contributor to notes.implicit.ly,
[send n8han an email][message] with a link to your Scala project if it
isn't on github.

Acquiring Credentials
---------------------

Herald authorizes directly with Tumblr so that you don't need to enter
any credentials. Run `herald` preview in a project directory and click
"Authorize with Tumblr" in the opened browser window. Approve Herald
to access the Tumblr account you wish to publish from.

Your authorization credentials are saved in `~/.herald` and may be
revoked at any time in your [Tumblr preferences][pref].

[pref]: https://www.tumblr.com/preferences

Publishing Release Notes
------------------------

Once you've previewed your source notes and released your project
binaries, you're ready to announce to the world. You can Publish right
from the preview browser, or from the command line.

    herald --publish

If the release notes publication is successful, the published URL will
open in the default broswer.

[posterous-sbt]: http://github.com/n8han/posterous-sbt
[simple-build-tool]: https://github.com/harrah/xsbt/wiki
[Posterous]: http://posterous.com/
[Knockoff]: http://tristanhunt.com/projects/knockoff/
[Dispatch]: http://dispatch.databinder.net/
[notes.implicit.ly]: http://notes.implicit.ly/
[plugins]: http://code.google.com/p/simple-build-tool/wiki/SbtPlugins
[message]: mailto:nathan@technically.us?subject=Requesting%20implicit.ly%20publishing%20rights
