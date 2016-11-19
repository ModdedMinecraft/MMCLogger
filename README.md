# MMCLogger-Sponge

# Features
- Log all chat interactions
- Log all commands
- Log each player's indavidual chat history seperatly (Optional)
- Customizable file format
- Notification in-game and logged to a file for notification commands & buzz words
- Command log blacklist to prevent overused / unnecessary commands being logged constantly
- Chat logs are sorted into folders for each month and named seperatly for each date.
- Chatlog folder is located in the main directory

# Permissions
- mmclogger.notify - Notifies the user of the buzz words defined in the config file.

# Upcoming features?
- Customizable folder location
- Log connections / disconections
- Sort command logs by month / date ?
- Changeable date format
- ???? 

# Config

log
command-log
- blacklist - A list of commands you do not want logged into the files.

LogFormat="[%date] %name: %content" - the log format for both chat and commands.

Formats avaliable: %date, %world, %x, %y, %z, %name, %content

Notifications 
- chat - A list of chat buzz words to notify any user with the permission and log it to a file.
- commands  - A list of command buzz words to notify any user with the permission and log it to a file.

Toggle 
- global-chat=true - log all chat
- global-commands=true - log all commands except for the blacklist
- global-login=true - log all player connections / disconections into the main chatlog files
- in-Game-notifications=true - notify any user with the permissions in game of the buzz words
- log-notify-chat=true - log the buzz words for the chat list
- log-notify-commands=true - log the buzz words for the commands list
- player-chat=true - log indavidual player files for chat
- player-commands=true - log indavidual player files for commands
- player-login=true - Upcomming feature - log player connections / disconections into indavidual files

# Credits
- Based from the same idea of CCLogger(Bukkit) by Alrik94

