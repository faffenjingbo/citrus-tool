/*
 * Copyright (c) 2002-2012 Alibaba Group Holding Limited.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.antx.util.cli;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>
 * <code>Parser</code> creates {@link CommandLine}s.
 * </p>
 *
 * @author John Keyes (john at integralsource.com)
 * @see Parser
 */
public abstract class Parser implements CommandLineParser {
    /** commandline instance */
    private CommandLine cmd;

    /** current Options */
    private Options options;

    /** list of required options strings */
    private List requiredOptions;

    /**
     * <p>
     * Subclasses must implement this method to reduce the
     * <code>arguments</code> that have been passed to the parse method.
     * </p>
     *
     * @param opts            The Options to parse the arguments by.
     * @param args            The arguments that have to be flattened.
     * @param stopAtNonOption specifies whether to stop flattening when a non
     *                        option has been encountered
     * @return a String array of the flattened arguments
     */
    protected abstract String[] flatten(Options opts, String[] arguments, boolean stopAtNonOption);

    /**
     * <p>
     * Parses the specified <code>arguments</code> based on the specifed
     * {@link Options}.
     * </p>
     *
     * @param options   the <code>Options</code>
     * @param arguments the <code>arguments</code>
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the arguments.
     */
    public CommandLine parse(Options options, String[] arguments) throws ParseException {
        return parse(options, arguments, false);
    }

    /**
     * <p>
     * Parses the specified <code>arguments</code> based on the specifed
     * {@link Options}.
     * </p>
     *
     * @param options         the <code>Options</code>
     * @param arguments       the <code>arguments</code>
     * @param stopAtNonOption specifies whether to stop interpreting the
     *                        arguments when a non option has been encountered and to add
     *                        them to the CommandLines args list.
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the arguments.
     */
    public CommandLine parse(Options opts, String[] arguments, boolean stopAtNonOption) throws ParseException {
        // initialise members
        options = opts;
        requiredOptions = options.getRequiredOptions();
        cmd = new CommandLine();

        boolean eatTheRest = false;

        List tokenList = Arrays.asList(flatten(opts, arguments, stopAtNonOption));
        ListIterator iterator = tokenList.listIterator();

        // process each flattened token
        while (iterator.hasNext()) {
            String t = (String) iterator.next();

            // the value is the double-dash
            if ("--".equals(t)) {
                eatTheRest = true;
            }
            // the value is a single dash
            else if ("-".equals(t)) {
                if (stopAtNonOption) {
                    eatTheRest = true;
                } else {
                    cmd.addArg(t);
                }
            }
            // the value is an option
            else if (t.startsWith("-")) {
                if (stopAtNonOption && !options.hasOption(t)) {
                    eatTheRest = true;
                    cmd.addArg(t);
                } else {
                    processOption(t, iterator);
                }
            }
            // the value is an argument
            else {
                cmd.addArg(t);

                if (stopAtNonOption) {
                    eatTheRest = true;
                }
            }

            // eat the remaining tokens
            if (eatTheRest) {
                while (iterator.hasNext()) {
                    String str = (String) iterator.next();

                    // ensure only one double-dash is added
                    if (!"--".equals(str)) {
                        cmd.addArg(str);
                    }
                }
            }
        }

        checkRequiredOptions();
        return cmd;
    }

    /**
     * <p>
     * Throws a {@link MissingOptionException} if all of the required options
     * are no present.
     * </p>
     */
    private void checkRequiredOptions() throws MissingOptionException {
        // if there are required options that have not been
        // processsed
        if (requiredOptions.size() > 0) {
            Iterator iter = requiredOptions.iterator();
            StringBuffer buff = new StringBuffer();

            // loop through the required options
            while (iter.hasNext()) {
                buff.append(iter.next());
            }

            throw new MissingOptionException(buff.toString());
        }
    }

    public void processArgs(Option opt, ListIterator iter) throws ParseException {
        // loop until an option is found
        while (iter.hasNext()) {
            String var = (String) iter.next();

            // found an Option
            if (options.hasOption(var)) {
                iter.previous();
                break;
            }
            // found a value
            else if (!opt.addValue(var)) {
                iter.previous();
                break;
            }
        }

        if (opt.getValues() == null && !opt.hasOptionalArg()) {
            throw new MissingArgumentException("no argument for:" + opt.getOpt());
        }
    }

    private void processOption(String arg, ListIterator iter) throws ParseException {
        // get the option represented by arg
        Option opt = null;

        boolean hasOption = options.hasOption(arg);

        // if there is no option throw an UnrecognisedOptionException
        if (!hasOption) {
            throw new UnrecognizedOptionException("Unrecognized option: " + arg);
        } else {
            opt = options.getOption(arg);
        }

        // if the option is a required option remove the option from
        // the requiredOptions list
        if (opt.isRequired()) {
            requiredOptions.remove("-" + opt.getOpt());
        }

        // if the option is in an OptionGroup make that option the selected
        // option of the group
        if (options.getOptionGroup(opt) != null) {
            OptionGroup group = options.getOptionGroup(opt);

            if (group.isRequired()) {
                requiredOptions.remove(group);
            }

            group.setSelected(opt);
        }

        // if the option takes an argument value
        if (opt.hasArg()) {
            processArgs(opt, iter);
        }

        // set the option on the command line
        cmd.addOption(opt);
    }
}
