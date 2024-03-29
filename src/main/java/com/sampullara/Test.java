package com.sampullara;

import com.sampullara.cli.Args;

@Dependency(groupid = "com.github.spullara.cli-parser", artifactid = "cli-parser", version = "1.1.1")
public class Test {
    public static void main(String[] args) {
        Args.usage(Test.class);
    }
}
