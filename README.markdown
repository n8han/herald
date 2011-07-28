posterous-sbt plugin
====================

**posterous-sbt** is a plugin for [simple-build-tool] that automates publishing release notes to any [Posterous] site, but especially [implicit.ly]. It transforms notes from Markdown to HTML using [Knockoff], and posts them using [Dispatch].

sbt 0.10.x
----------

### As a global dependency

If you install posterous-sbt globally, you can use it with any project
without having to make any changes to that project's definition.

***`~/.sbt/plugins/build.sbt`***

```scala
libraryDependencies <+= (sbtVersion) { sv =>
  "net.databinder" %% "posterous-sbt" % ("0.3.0_sbt" + sv)
}
```

### User settings

Once this is done, you'll need to set your Posterous email and
password before you can publish release notes.

**~/.sbt/user.sbt**

```scala
(email in Posterous) := Some("you@example.com")

(password in Posterous) := Some("yourpassword")
```

### Project-specific settings

If you wish to customize any settings for your project and commit
those into source control, you *must* add posterous-sbt as a project
plugin; otherwise, anyone who checks out the project and doesn't have
the plugin globally will be unable to build.

To add posterous-sbt as a project dependency, use the same
`libaryDependencies` as shown for the global configuration, but place
it in `project/plugins/build.sbt` under your project directory. Then,
you can safely reference posterous settings inside your project's
build definition. e.g., to change the published project name:

```scala
(name in Posterous) := "A Friendlier Name"
```

sbt 0.7
-------

To use this plugin with an sbt 0.7.x project,
[declare it as a dependency][plugins] in a file under
`project/plugins`. e.g. `project/plugins/Plugins.scala` with:

    import sbt._

    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val posterous = "net.databinder" % "posterous-sbt" % "0.1.7"
    }

And then it mix the trait into a project definition:

    class MyProject(info: ProjectInfo) extends PluginProject(info) with posterous.Publish ...

For this older version of the plugin, your Posterous credentials are set in a java properties file `~/.posterous` :

    email=me@example.com
    password=mypassword

Notes Specification
-------------------

Release notes for your project are expected under a `notes/` directory in the root of the project that mixes in the `posterous.Publish` trait. In this directory, notes for a particular version are named `<version>.markdown` while an optional short description about the project can be placed in `about.markdown`. For example, in posterous-sbt you'll find the following:

    posterous-sbt/
      notes/
        0.1.0.markdown
        0.1.1.markdown
        about.markdown

When publishing and previewing, the description from `about.markdown` is appended to the selected version's release notes as boilerplate.

If you're publishing to the Scala software announcement site [implicit.ly], please keep in mind that the post's title will be the name of your project and its corresponding version: these should not be repeated as a heading in the notes. It is best to lead with copy describing the big changes in your release, or jump right into a list of those changes. For major releases with changes divided into sections, use an `h3` (a line prefixed by `###` in Markdown) or smaller heading. The short description `about.markdown` should be one or two sentences long, with a link to more information about your project.

To **preview** the transformed release notes, run the
`posterous:preview` (`preview-notes` in sbt 0.7) action in sbt. This
looks for the current version with any "-SNAPSHOT" suffix removed; The
notes should open in your default browser.

Publication Target Site
-----------------------

This plugin comes preconfigured to publish to [implicit.ly]. To be
added as a contributor to implicit.ly,
[send n8han a message on github][message] **containing your email
address** and a link to your Scala project if it isn't on github. If
you'd like to publish to a different Posterous site, just override the
`posterous:siteId` setting.

You'll need to create a login on [Posterous] if you haven't done that
yet. See above, under sbt 0.10 or 0.7, for instructions.

Once you've identified yourself, you can check your setup with the
`posterous:check` action (`check-posterous` in sbt 0.7). This will
fetch your list of authorized sites from Posterous and confirm that
the project's current `siteId` is one of those.

Publishing Release Notes
------------------------

Once you've previewed your source notes and checked your publishing
authorization, you're ready to post to the web. Like the
`posterous:preview` action, `posterous:publish` (`publish-notes` in
sbt 0.7) uses the current non-snapshot version.

If the release notes publication is successful, the shortened
published URL is displayed and will open in the default broswer.

[posterous-sbt]: http://github.com/n8han/posterous-sbt
[simple-build-tool]: https://github.com/harrah/xsbt/wiki
[Posterous]: http://posterous.com/
[Knockoff]: http://tristanhunt.com/projects/knockoff/
[Dispatch]: http://dispatch.databinder.net/
[implicit.ly]: http://implicit.ly/
[plugins]: http://code.google.com/p/simple-build-tool/wiki/SbtPlugins
[message]: http://github.com/inbox/new/n8han
