# COMP2300 Group 33 - Energy App

## How to Use This Repo

### Cloning

You will want to clone in to this Gitlab repository on your own machines using IntelliJ. The best way to do this is inside the IntelliJ Idea application itself.

**Steps:**

1. Navigate to the IntelliJ Idea welcome screen. In top menu, select File > New > Project from Version Control...
2. You will now be on a window titled "Clone Repository". Go to the Gitlab page for the energy app project and select the blue Code button. Copy the HTTPS link and paste it into the box in the clone repository screen.
3. You will now see a screen asking for a token. Press the generate button, which will take you to the Soton Gitlab site. Create a token, ensuring `api` and `read_user` options are ticked.
4. Copy the token that is given to you, and save it somewhere as you can't access it later. Paste that token into the box in the IntelliJ window.
5. Hit trust project, and you should be done! Try running the project to ensure everything worked properly.

### FXML Ground Rules
This section outlines some basic ground rules for creating FXML UI blueprints in SceneBuilder. This is **crucial**, because will ensure we don't hit hard to debug Null errors later.

#### Directory Structure
To ensure Java can find the files it needs, stick to this structure:

- **Java Controllers**: `src/main/java/com/energyapp/controller/`
- **FXML Views**: `src/main/resources/com/energyapp/view/`
- **Images and Assets**: `src/main/resources/com/images/`
    - **Note for images**: *NEVER* use local absolute paths when adding images into the UI of the app. Always save the image to the above path first, and then use it.

#### Setting `fx:id`

It is important to do this so we can reference FXML elements when we write the Java logic for them. In SceneBuilder:

1. Expand the "Code" drop-down, located on the right of the SceneBuilder window
2. Locate the `fx:id` attribute. *Make sure you are not editing the CSS ID which is in Properties*.
3. Change this field to a sensible, descriptive name.

#### Setting Controller Class Paths

This is needed to make sure the FXML files know what Java class controls them. **ONLY do this to the root element of the file you are working on in SceneBuilder**. Do not do this on any other element.

1. Expand the "Controller" drop-down, located in the bottom left of the SceneBuilder window.
2. Locate the `Controller class` attribute. This is normally right at the top.
3. Change this field to `com.energyapp.controller.[AssociatedControllerClassNameGoesHere]`

#### Refreshing Controllers

If you happen to add or rename an `fx:id` in SceneBuilder or manually in the FXML file, you need to update the Java Controller file to make sure it has the correct reference to the element you changed.

Imagine you changed a `Button` to have `fx:id="submitButton"`. Go to the associated controller class, and make sure you have this code:

```java
@FXML
private Button submitButton; // Name must be an exact match to the fx:id
```

## Testing

### Using `App.java` for Test Runs

When you modify `App.java` to test run a feature you have created, make sure you DON'T push your changes to `App.java` to main. The version of `App.java` on main is build specifically to load the main layout and start the time simulation - don't change this unless you are specifically making changes to `App.java`

Therefore, when you modify this file in your own branch, make sure you **do not tick `App.java` when you commit and push**.

## Installation and Execution (for marker)

### Prerequisites

Ensure you have the following installed on your machine:
- **Java Development Kit 17** (or higher)
- **Maven 3.6.0** (or higher)

### Running via Fat Jar
We've included a fat jar file that you can find in the project root. You can just double-click this and the project should run!

### Running via Command Line
1. Open terminal or cmd
2. Navigate to root directory of project
3. Run this command:
   ```bash
   mvn clean javafx:run
   ```