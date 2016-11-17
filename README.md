# MMCLogger-Sponge

# Features
- Log all chat interactions
- Log all commands
- Log each player's indavidual chat history seperatly (Optional)
- Customizable file format
- Notification in-game and logged to a file for notification commands & buzz words
- Commandlog blacklist to prevent overused / unnecessary commands being logged constantly 
- Chat logs are sorted into folders for each month and named seperatly for each date.
- Chatlog folder is located in the main directory

# Permissions
- mmclogger.notify - Notifies the user of the buzz words defined in the config file.

# Upcoming features?
- Customizable folder location
- Log connections / disconections
- Sort command logs by month / date ?
- ????

# Config

Log
CommandLog 
Blacklist - A list of commands you do not want logged into the files.

LogFormat="[%date] %name: %content"

Notifications 
Chat - A list of chat buzz words to notify any user with the permission and log it to a file.
Commands  - A list of command buzz words to notify any user with the permission and log it to a file.

Toggle 
GlobalChat=true - log all chat
GlobalCommands=true - log all commands except for the blacklist
GlobalLogin=true - log all player connections / disconections into the main chatlog files
InGameNotifications=true - notify any user with the permissions in game of the buzz words
LogNotifyChat=true - log the buzz words for the chat list
LogNotifyCommands=true - log the buzz words for the commands list
PlayerChat=true - log indavidual player files for chat
PlayerCommands=true - log indavidual player files for commands
PlayerLogin=true - Upcomming feature - log player connections / disconections into indavidual files

# Credits
-Based from the same idea of CCLogger(Bukkit) by Alrik94

