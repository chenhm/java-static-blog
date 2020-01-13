package com.chenhm.blog;

import com.beust.jcommander.Parameter;

import lombok.Data;

@Data
public class Args {
    @Parameter(names = {"-d", "--dev"}, description = "Run as dev model, the generator will watch your source folder and re-render blog when you change source files")
    private boolean dev = false;

//    @Parameter(names = "--debug", description = "Debug mode", arity = 1)
//    private boolean debug = false;

    @Parameter(names = "--pdf", description = "Enable pdf renderer")
    private boolean pdf = false;

    @Parameter(names = {"-s"}, description = "Blog base directory")
    private String source = ".";

    @Parameter(names = {"-h", "--help"}, help = true, description = "Print this help")
    private boolean help;

}
