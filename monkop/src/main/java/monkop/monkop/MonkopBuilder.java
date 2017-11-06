package monkop.monkop;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Oscar Calderin
 */
public class MonkopBuilder extends Builder implements SimpleBuildStep {

    private final String path;
    private final String apk;
    private final String secretKey;
    private final Integer time;
    private final boolean wait;

    @DataBoundConstructor
    public MonkopBuilder(String path, String apk, String secretKey,
                         Integer time, boolean wait)
    {
        this.path = path;
        this.apk = apk;
        this.secretKey = secretKey;
        this.time = time;
        this.wait = wait;
    }

    public String getPath() {
        return path;
    }

    public String getApk() {
        return apk;
    }

    private String getSecretKey() {
        return secretKey;
    }

    private Integer getTime() {
        return time;
    }

    private boolean getWait() {
        return wait;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        if(getDescriptor().getUseMonkop()){
            ArgumentListBuilder command = new ArgumentListBuilder();
            String commandLine = "python ";
            String separator = ( Os.isFamily(Os.FAMILY_WINDOWS) ) ? "\\" : "/";
            String tempPath = ( getPath().endsWith(separator) )? getPath() + "monkop-cli.py" : getPath() + separator + "monkop-cli.py";
            commandLine += tempPath + " -k " + getSecretKey() + " -t " + getTime();
            if(getWait()){
                commandLine += " -w ";
            }
            commandLine += " -a " + getApk();
            command.addTokenized(commandLine);
            Launcher.ProcStarter ps = launcher.new ProcStarter();
            ps = ps.cmds(command).stdout(listener);
            ps = ps.pwd(build.getRootDir()).envs(build.getEnvironment(listener));
            Proc proc = launcher.launch(ps);
            int retcode = proc.join();
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private boolean useMonkop;

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckForm(@QueryParameter("path") final String _path,
                                          @QueryParameter("apk") final String _apk,
                                          @QueryParameter("secretKey") final String _secretKey,
                                          @QueryParameter("time") final String _time
                                          )
                throws IOException, ServletException
        {
            if (_path.length() == 0 || _apk.length() == 0 || _secretKey.length() == 0 ||
                    _time.length() == 0)
                return FormValidation.error("Please set a value for all fields");
            int tTime = Integer.parseInt(_time);
            if (tTime <= 0)
                return FormValidation.error("Please set a value for Wait for completion field");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Execute Monkop";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            useMonkop = formData.getBoolean("useMonkop");
            save();
            return super.configure(req,formData);
        }

        private boolean getUseMonkop() {
            return useMonkop;
        }

    }
}

