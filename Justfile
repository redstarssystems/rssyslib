GROUP_ID := env_var('GROUP_ID')
ARTIFACT_ID := env_var('ARTIFACT_ID')
VERSION_ID := `cat version_id`

TARGET_FOLDER := 'target'

YELLOW_COLOR := '\033[0;33m'
GREEN_COLOR := '\033[0;92m'
NORMAL_TEXT := '\033[0m'
CURRENT_TIME := `date`
CURRENT_OS := os()

##################################################################################################################

# Help
default:
    @echo "List of available recipes"
    @just --list

# Install project requirements (OS will be detected automatically)
requirements:
    _@{{ if CURRENT_OS == 'macos' {"just macos-requirements" } else {""} }}
    _@{{ if CURRENT_OS == 'linux' {"just linux-requirements" } else {""} }}

# Install project requirements for MacOS
_macos-requirements:
    _@just _cmdprint "Installing MacOS requirements...\n"
    -brew install git
    -brew install coreutils
    -brew install direnv
    -brew install jenv
    -brew install cljstyle && xattr -r -d com.apple.quarantine /usr/local/bin/cljstyle

    _@just cprint '{{YELLOW_COLOR}}' "\nDon't forget to install 'direnv' & 'jenv' hooks for your shell.\n"
    @echo 'zsh hooks example:'
    @echo '\texport PATH="$HOME/.jenv/bin:$PATH"'
    @echo '\teval "$(jenv init -)"'
    @echo '\teval "$(direnv hook zsh)"\n'


# Install project requirements for Linux
_linux-requirements:
    @echo "Installing Linux requirements...is not implemented."
    @echo "Please, ensure that following tools are installed: git, direnv, jenv, cljstyle."
    @exit 1

# Print command name
_cmdprint text:
    #!/usr/bin/env bb
    (println "-----------------------------------------------")
    (import 'java.time.format.DateTimeFormatter 'java.time.LocalDateTime)
    (def date (LocalDateTime/now))
    (def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))
    (printf "{{GREEN_COLOR}}%s\n"(.format date formatter))
    (println "{{YELLOW_COLOR}}{{text}}{{NORMAL_TEXT}}")

_cprint color text:
    #!/usr/bin/env bb
    (println "{{color}}{{text}}{{NORMAL_TEXT}}")

##################################################################################################################

# Clean target folder
clean:
    @just _cmdprint "Clean target folder...\n"
    @rm -rf ./{{TARGET_FOLDER}}/*


# Build deployable jar file of this project
build:
    @just _cmdprint "Building project...\n"
    @mkdir -p target
    clojure -X:jar :jar {{TARGET_FOLDER}}/{{GROUP_ID}}-{{VERSION_ID}}.jar :group-id {{GROUP_ID}} :artifact-id {{ARTIFACT_ID}} :version '"{{VERSION_ID}}"'


# Install deployable jar locally (requires the pom.xml file)
install:
    @just _cmdprint "Installing jar file to local .m2 repository...\n"
    clojure -X:install :installer :local :artifact '"{{TARGET_FOLDER}}/{{GROUP_ID}}-{{VERSION_ID}}.jar"'


# Deploy this library to Clojars
deploy:
    @just _cmdprint "Deploying jar file to Clojars...\n"
    clojure -X:install :installer :remote :artifact '"{{TARGET_FOLDER}}/{{GROUP_ID}}-{{VERSION_ID}}.jar"'

# Run Clojure repl
repl:
    @just _cmdprint "Running Clojure repl...\n"
    @clojure -M:repl


# Check for outdated dependencies
outdated:
    @just _cmdprint "Checking for outdated dependencies...\n"
    @clojure -M:outdated
