package win.scolia.sso.bean.vo.export;

import java.util.Date;

/**
 * 用户信息的输出VO
 */
public class UserExportVO {

    private Long userId;

    private String userName;

    private Date createTime;

    private Date lastModified;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "UserExportVO{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", createTime=" + createTime +
                ", lastModified=" + lastModified +
                '}';
    }
}