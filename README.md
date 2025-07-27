# GtbWords
### 爬取Crowdin上Hypixel翻译项目中建筑猜猜乐词库的爬虫程序。
###### **如你所见，本项目使用的是 _WTFPL_ 。也就是说你可以~~爱剂把干啥干啥~~**

---
## 使用方法

0. 准备所需账号和运行环境
    - **Crowdin账号** 且已经加入Hypixel简体中文汉化组
      需要的参数有
      - `email`: 邮箱地址
      - `crowdinPassword`: Crowdin密码
    - **Outlook令牌卡** 可以登录上述Crowdin账号的邮箱  
      令牌卡应当包含的参数有  
      - `email`: 邮箱地址，<u>**需与Crowdin账号邮箱一致**</u>
      - `clientId`: 令牌卡的客户端UUID
      - `refreshToken`: 令牌卡的刷新令牌
    - **Mozilla Firefox**
    - **JDK 21**
    - **Maven**  
   你应当将`email`,~~`emailPassword`~~,`crowdinPassword`,`clientId`,`refreshToken`编写按[account.json.template](src/main/resources/account.json.template)所展示格式的json文件，放在[src/main/resources/account.json](src/main/resources/account.json)
1. 运行程序
    - **Github/Gitea Actions**
      + 修改Github/Gitea Action Secret，添加`ACCOUNT`变量，值为[account.json]()文件内容
      + 设法触发**push事件**以启动Actions *瞎寄吧写点啥不影响功能的提交推上去就行*
      + 在工件处获取成品[result.csv]()
    - **本地运行**  
    在本地运行只需要两步。
    ```shell
    mvn clean package                           # 编译 打包
    java -jar target/GtbWords-1.0-SNAPSHOT.jar  # 运行
    ```
你可以在与jar包相同目录下找到[result.csv]()。  
    

2. 运行结果

| 审核状态(T/F) | 英语          | 中文 |
|-----------|-------------|----|
| T         | Angel       | 天使 | 
| F         | Board Games | 桌游 | 
- 审核状态`F` = 已翻译但未通过审核
- 审核状态`T` = 已通过审核