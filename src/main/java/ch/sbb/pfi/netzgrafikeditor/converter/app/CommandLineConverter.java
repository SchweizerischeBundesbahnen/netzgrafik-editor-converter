package ch.sbb.pfi.netzgrafikeditor.converter.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class CommandLineConverter implements CommandLineRunner, ExitCodeGenerator {

    private static final String FOOTER_KEY = "footer";

    private final CommandLine.IFactory factory;
    private final ConvertCommand convertCommand;
    private int exitCode;

    CommandLineConverter(CommandLine.IFactory factory, ConvertCommand convertCommand) {
        this.factory = factory;
        this.convertCommand = convertCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(CommandLineConverter.class, args)));
    }

    @Override
    public void run(String... args) {
        CommandLine commandLine = new CommandLine(convertCommand, factory);
        commandLine.getHelpSectionMap().put(FOOTER_KEY, convertCommand.new FooterProvider());
        exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
