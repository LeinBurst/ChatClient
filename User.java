public class User{
  private String nickname;
  private String state;
  private String room;
  private String buffer;

  User(String n) {
    nickname = n;
    state = "init";
    room = "";
    buffer = "";
  }

  User() {
    nickname = "";
    state = "init";
    room = "";
    buffer = "";
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String n) {
    nickname = n;
  }

  public String getState() {
    return state;
  }

  public void setState(String s) {
    state = s;
  }
  public String getRoom() {
    return room;
  }

  public void setRoom(String r) {
    room = r;
  }
  public String getBuffer() {
    return buffer;
  }

  public void setBuffer(String b) {
    buffer = b;
  }
}
