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

Log {
    CommandLog {
        Blacklist=[
            help,
            who,
            home
        ]
    }
    LogFormat="[%date] %name: %content"
    Notifications {
        Chat=[
            ddos,
            hack,
            flymod,
            dupe,
            duplicate,
            duplication
        ]
        Commands=[
            item,
            give,
            sponge,
            op
        ]
    }
    Toggle {
        GlobalChat=true
        GlobalCommands=true
        GlobalLogin=true
        InGameNotifications=true
        LogNotifyChat=true
        LogNotifyCommands=true
        PlayerChat=true
        PlayerCommands=true
        PlayerLogin=true
    }
}

# Credits
-Based from the same idea of CCLogger(Bukkit) by Alrik94

