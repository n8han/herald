posterous-sbt plugin
====================

[posterous-sbt] is a plug-in for [simple-build-tool] that automates publishing release notes to any [Posterous] site.

Notes Specification
-------------------

Release notes for your project are expected under a `note/` directory in the root of the project that mixes in the `posterous.Publish`. In this directory, notes for a particular version are named `<version>.markdown` while an optional short description about the project can be placed in `about.markdown.`. For example, the posterous-sbt project itself uses posterous-sbt to publish its notes:

    posterous-sbt/
      notes/
        0.1.0.markdown
        0.1.1.markdown
        about.markdown

When publishing, the description from `about.markdown` is appended to the selected version's release notes.

If you're publishing to the Scala software announcement site [implicit.ly], we ask that you keep your notes brief and avoid unnecessary section headings. The post's title will be the name of your project and its corresponding version; this should not be repeated as a heading in the notes. It is best to lead with copy describing the big changes in the release, or jump right into a list of those changes. For major releases with changes divided into sections, please use an h3 (a line prefixed by `###` in Markdown) or smaller heading.

[posterous-sbt]: http://github.com/n8han/posterous-sbt
[simple-build-tool]: http://code.google.com/p/simple-build-tool/
[Posterous]: http://posterous.com/
[implicit.ly]: http://implicit.ly/