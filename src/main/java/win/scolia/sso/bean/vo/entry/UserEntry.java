package win.scolia.sso.bean.vo.entry;

import javax.validation.constraints.NotNull;

/**
 * 用户信息的输入VO对象
 */
public class UserEntry {

    // 注册组
    public interface Register {}

    // 修改密码组
    public interface ChangePassword{}

    @NotNull(message = "用户名不能为空", groups = {Register.class, ChangePassword.class})
    private String userName;

    @NotNull(message = "密码不能为空", groups = {Register.class})
    private String password;

    @NotNull(message = "旧密码不能为空", groups = {ChangePassword.class})
    private String oldPassword;

    @NotNull(message = "新密码不能为空", groups = {ChangePassword.class})
    private String newPassword;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "UserEntryVO{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", oldPassword='" + oldPassword + '\'' +
                ", newPassword='" + newPassword + '\'' +
                '}';
    }
}



