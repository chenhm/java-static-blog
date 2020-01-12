package com.chenhm.blog;

import com.beust.jcommander.Parameter;

import lombok.Data;

@Data
public class Args {
    @Parameter(names = {"-d", "--dev"}, description = "run as dev model")
    private boolean dev = false;

    @Parameter(names = "--debug", description = "Debug mode", arity = 1)
    private boolean debug = false;

    @Parameter(names = {"-s"}, description = "blog source directory")
    private String source = ".";

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help;

}
