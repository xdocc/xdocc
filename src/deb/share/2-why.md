## Why anoter static site generator? (this section is written in Markdown)

On [StaticGen](https://www.staticgen.com), there is a list with over 200 static site generators. This begs the question why do we need another one? The word "standards" in the following [xkcd](https://xkcd.com/927/) can be replaced by "static site generators", which sums up why to create another one:

![xkcd center](${root}/standards.png)

One key issue with current static site generators is that they do not work **incrementally**. For sites generating markdowns to HTML, this is not an issue, however, if you want to also generate a gallery, e.g., from your last trip, incremental generation is necessary. Even with a couple of hundred pictures it can take up many minutes to resize them. A small update, unrelated to the picture itself should never trigger a full site generation.

Another goal is to keep the code size as small as possible and reuse existing libraries. Xdocc can be seen as a glue between many libraries and the xdocc code itself has currently less than 3500 lines of code. The goal is to reach 3000 lines of code. Less code means less maintenance (but relying more on external libraries).