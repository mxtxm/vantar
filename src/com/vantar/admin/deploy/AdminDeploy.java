package com.vantar.admin.deploy;

import com.vantar.admin.index.Admin;
import com.vantar.common.Settings;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.*;
import com.vantar.util.os.Command;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;

/**
 * EXPERIMENTAL
 */
public class AdminDeploy {

    public static void upload(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("DEPLOY - upload", params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginUploadForm()
                .addFile("WAR", "file")
                .addSubmit(VantarKey.ADMIN_SUBMIT)
                .finish();
            return;
        }

        try (Params.Uploaded uploaded = params.upload("file")) {
            if (!uploaded.isUploaded() || uploaded.isIoError()) {
                ui.addErrorMessage(VantarKey.REQUIRED, "file").finish();
                return;
            }

            if (uploaded.moveTo(Settings.config.getProperty("deploy.path"), "ROOT-"
                + new DateTime().formatter().getDateTimeAsFilename() + ".war")) {

                DirUtil.giveAllPermissions(Settings.config.getProperty("deploy.path"));
                ui.addMessage("done!");
            } else {
                ui.addMessage(VantarKey.UPLOAD_FAIL);
            }
        }

        ui.finish();
    }

    public static void deploy(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("DEPLOY", params, response, true);

        String filepath = params.getString("file");
        String password = params.getString("password");
        if (filepath != null) {
            try {
                if (password == null) {
                    ui.addBlock("pre", Command.run("service tomcat9 stop"));
                    ui.addBlock("pre", Command.run("rm -r /var/lib/tomcat9/webapps/*"));
                    ui.addBlock("pre", Command.run("cp " + filepath + " /var/lib/tomcat9/webapps/ROOT.war"));
                    ui.addBlock("pre", Command.run("service tomcat9 start"));
                } else {
                    ui.addBlock("pre", Command.run("service tomcat9 stop", password));
                    ui.addBlock("pre", Command.run("rm -r /var/lib/tomcat9/webapps/*", password));
                    ui.addBlock("pre", Command.run("cp " + filepath + " /var/lib/tomcat9/webapps/ROOT.war", password));
                    ui.addBlock("pre", Command.run("service tomcat9 start", password));
                }
            } catch (Exception e) {
                ui.addErrorMessage(e);
            }
            ui.finish();
            return;
        }

        for (String path : DirUtil.getDirectoryFiles(Settings.config.getProperty("deploy.dir"))) {
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];

            ui.addBlockNoEscape(
                "div",
                ui.getHref(
                    filename + " (" + FileUtil.getSizeMb(path) + "Mb)",
                    "/admin/deploy?" + "file" + "=" + filename + "&password=" + password,
                    false,
                    false,
                    ""
                ),
                "box-content"
            ).write();
        }

        ui.finish();
    }

    public static void shell(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("SHELL", params, response, true);

        String command = params.getString("command");
        String password = params.getString("password");

        if (command != null) {
            try {
                if (password == null) {
                    ui.addBlock("pre", Command.run(command));
                } else {
                    ui.addBlock("pre", Command.run(command, password));
                }
            } catch (Exception e) {
                ui.addErrorMessage(e);
            }
            ui.finish();
            return;
        }

        ui  .beginFormPost()
            .addInput("password", "password")
            .addInput("command", "command")
            .addSubmit(VantarKey.ADMIN_SUBMIT)
            .finish();
    }
}
