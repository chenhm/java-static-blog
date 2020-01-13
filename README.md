# Java static blog

!["Build Status", link="https://travis-ci.org/chenhm/java-static-blog"](https://img.shields.io/travis/chenhm/java-static-blog/master.svg)

This is a Java based static blog generator support Markdown and Asciidoc.

I like both the simplicity of Markdown and the rich expressiveness of Asciidoc, but all blog systems that support both markup languages are too complicated. A few years ago, I adopted a pure front-end solution [vue-github-blog](https://github.com/chenhm/vue-github-blog), but because Asciidoc's parser is very complicated, some clients parsing speed is not as fast as expected. Furthermore, browser need to read resources directly from the github repository, there are sometimes failures in China. This Java version only generates pure static pages. With CDN acceleration, you can get good access results.

我既喜欢 Markdown 的简洁又喜欢 Asciidoc 的丰富表达力，但同时支持这两种标记语言的博客系统都太复杂了。前几年我采用了一个纯前端的解决方案 [vue-github-blog](https://github.com/chenhm/vue-github-blog), 但因为 Asciidoc 的解析器非常复杂，导致部分客户端的解析速度不理想，再者需要直接从 github 仓库读取资源，在国内时不时会遇到一些障碍。这个 Java 版本只生成纯静态页面，配合CDN加速，可以获得不错的访问效果。

## Usage 使用方法

1. Generate blog
   
   Put your `md` and `adoc` files into `posts` folder, then run below commands to generate.
   ```bash
   curl -vL "https://github.com/chenhm/java-static-blog/releases/download/v0.0.1/java-static-blog-0.0.1.jar" -o java-static-blog.jar
   java -Dfile.encoding=UTF-8 -jar java-static-blog.jar
   ```

2. Preview blog
   
   Different markdown / asciidoc parsers will have different effects, please use the `-d` parameter to preview the generated effect before publishing, e.g: `java -Dfile.encoding=UTF-8 -jar java-static-blog.jar -d`

3. Custom parameters
   
   Please set custom parameters by Java options, e.g: `-Dapp.post-path=<your path>`, the more parameter items please refer `application.properties`. 

Please refer https://github.com/chenhm/blog project and it's `.travis.yml`.

<hr/>

1. 生成博客

    在 `posts` 目录下放入 `md` 或 `adoc` 文件，然后执行以下命令即可在 `dist` 目录下生成静态博客。
    ```bash
    curl -vL "https://github.com/chenhm/java-static-blog/releases/download/v0.0.1/java-static-blog-0.0.1.jar" -o java-static-blog.jar
    java -Dfile.encoding=UTF-8 -jar java-static-blog.jar
    ```

2. 预览博客
   
   不同的 markdown/asciidoc 解析器在实现上会有细微差别，在发布前建议使用 `-d` 参数预览生成效果，例如 `java -Dfile.encoding=UTF-8 -jar java-static-blog.jar -d`

3. 自定义参数
   
   参考 `application.properties` 文件，通过 Java options 设置参数， 例如： `-Dapp.post-path=<your path>`

详细用法可以参考 https://github.com/chenhm/blog 和对应的 `.travis.yml`。

## Tools & Libraries Used 使用的工具

* [AsciidoctorJ](http://asciidoctor.org/)
* [Flexmark](https://github.com/vsch/flexmark-java)
* [handlebars.java](http://jknack.github.io/handlebars.java/)
* [JCommander](https://jcommander.org/)
* [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
