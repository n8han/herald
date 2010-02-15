posterous-sbt plugin
====================

[posterous-sbt] is a plugin for [simple-build-tool] that automates publishing release notes to any [Posterous] site. It transforms notes from Markdown to HTML using [Knockoff], and posts them using [Dispatch].

To use this plugin with an sbt project, [declare it as a dependency][plugins] in a file under `project/plugins`. Since posterous-sbt uses itself as a plugin, it has a file `posterous-sbt/project/plugins/Plugins.scala` with:

    import sbt._
    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
      val posterous = "net.databinder" % "posterous-sbt" % "0.1.1" // or latest version
    }

You can use this in conjunction with other plugins, just add them as dependencies to the same file.

Notes Specification
-------------------

Release notes for your project are expected under a `note/` directory in the root of the project that mixes in the `posterous.Publish`. In this directory, notes for a particular version are named `<version>.markdown` while an optional short description about the project can be placed in `about.markdown`. For example, in this project you'll find the following:

    posterous-sbt/
      notes/
        0.1.0.markdown
        0.1.1.markdown
        about.markdown

When publishing and previewing, the description from `about.markdown` is appended to the selected version's release notes as boilerplate.

If you're publishing to the Scala software announcement site [implicit.ly], please keep in mind that the post's title will be the name of your project and its corresponding version; this should not be repeated as a heading in the notes. It is best to lead with copy describing the big changes in the release, or jump right into a list of those changes. For major releases with changes divided into sections, use an `h3` (a line prefixed by `###` in Markdown) or smaller heading. The short description `about.markdown` should be one or two sentences long, with links to more information about your project.

To preview the transformed release notes, run the `preview-notes` action in sbt. This can be followed by a version number; the default is the current version with an "-SNAPSHOT" suffix removed. If you're on a 1.6+ JVM, the notes should open in your default browser. The location of the file they've been saved to is displayed in the sbt output.

Publication Target Site
-----------------------

This plugin comes preconfigured to publish to [implicit.ly]. To be added as a contributor to implicit.ly, [send me a message on github][message] containing your email address and a link to your Scala project if it isn't on github. If you'd like to publish to a different Posterous site, just override the `postSiteId` method in your project definition.

You'll need to create a login on [Posterous] if you haven't done that yet. Then, specify it in the file `~/.posterous`:

    email=me@example.com
    password=mypassword

Once you've done that, you can check your setup with the `check-posterous` action. This will fetch your list of authorized sites from Posterous and confirm that the project's current `postSiteId` is one of those.

Publishing a Release
--------------------

Once you've previewed your source notes and checked your publishing authorization, you're ready to post to the web. Like the `preview-notes` action, `publish-notes` defaults to the current non-snapshot version and can be override with a parameter. If publication is successful, the shortened published URL is displayed and will open in the default broswer on 1.6+ JVMs. Hello, world!

[posterous-sbt]: http://github.com/n8han/posterous-sbt
[simple-build-tool]: http://code.google.com/p/simple-build-tool/
[Posterous]: http://posterous.com/
[Knockoff]: http://tristanhunt.com/projects/knockoff/
[Dispatch]: http://dispatch.databinder.net/
[implicit.ly]: http://implicit.ly/
[plugins]: http://code.google.com/p/simple-build-tool/wiki/SbtPlugins
[message]: http://github.com/inbox/new/n8han