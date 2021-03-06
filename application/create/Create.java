package application.create;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import application.home.Home;
import application.learn.Question;
import application.learn.QuestionSet;
import application.main.Help;
import application.main.Main;
import application.popup.Popup;
import application.view.View;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Represents the Create tab of the WikiSpeak application. This class deals with the creation
 * process of the application, specifically searching, editing, and finalising the creation.
 * 
 *@author Jacinta, Lynette, Tushar
 */
public class Create {

	// GUI fields related to making the Create tab.
	private TabPane _tabPane;
	private Tab _tab;
	private View _view;
	private Main _main;

	private ImageManager _imMan;
	private SelectLines _selLines;
	private Popup _popup;

	private BooleanBinding _searchBinding;

	private Button _helpButton;
	private Button _searchButton;

	private HBox _searchBar;
	private VBox _contents;

	private TextField _search = new TextField();

	private Label _message = new Label();
	private Label _create = new Label();

	private ProgressIndicator _pbCombine = new ProgressIndicator();
	private ProgressIndicator _pbSave = new ProgressIndicator();
	private ProgressIndicator _pbSearch = new ProgressIndicator();
	private File _file;

	private int _numberOfAudioFiles = 0;
	private int _numberOfPictures;
	private ObservableList<String> _listLines = FXCollections.observableArrayList();

	private String _term;
	private String _name;
	private String _music;

	private final QuestionSet _set;

	private final String EMPTY = "Empty";
	private final String VALID = "Valid";
	private final String DUPLICATE = "Duplicate";
	private final String FESTIVAL = "Human";

	/**
	 * Sets up the Create tab to update View tab etc when a creation is made. Also
	 * gives Create a popup instance for its popups.
	 */
	public Create(Tab tab, Popup popup, QuestionSet set, View view) {
		_tab = tab;
		_popup = popup;
		_imMan = new ImageManager();
		_set = set;
		_view = view;
	}

	/**
	 * Sets the initial contents of the Create tab when the application is first started.
	 * The user can thus instantly switch to the Create tab when the app begins.
	 */
	public void setContents(Main main) {
		if (_main == null) {
			_main = main;
		}
		_create.setText("Enter word: ");

		_searchBinding = _search.textProperty().isEmpty();

		// Safe search to prevent users from searching inappropriate terms.
		// File retrieved from https://www.freewebheaders.com/full-list-of-bad-words-banned-by-google/
		File file = new File(".resources/search/badWords.txt"); 
		try {
			BufferedReader br = new BufferedReader(new FileReader(file)); 
			String st; 
			while ((st = br.readLine()) != null) {
				_searchBinding = _searchBinding.or(_search.textProperty().isEqualToIgnoreCase(st));
			}
			br.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		_searchButton = new Button("Search ???");
		// disable button if search field is empty/contains bad word
		_searchButton.disableProperty().bind(_searchBinding); 

		final Pane spacer = new Pane();
		spacer.setMinSize(1, 1);
		HBox.setHgrow(spacer, Priority.ALWAYS);

		_helpButton = new Button("?");
		_helpButton.setVisible(true);
		Help helpContents = new Help();
		ContextMenu cm = helpContents.getContextMenuCreate();
		_helpButton.setContextMenu(cm);

		_pbSearch.setVisible(false);
		_searchBar = new HBox(_create, _search, _searchButton, _pbSearch, spacer, _helpButton);
		_searchBar.setSpacing(10);

		// Enter shortcut-key to search
		_search.setOnKeyPressed(arg0 -> {if (arg0.getCode().equals(KeyCode.ENTER)) _searchButton.fire();});

		// Disable search function while search is processed
		_searchButton.setOnAction(e -> {
			_searchButton.disableProperty().unbind();
			_searchButton.setDisable(true);
			searchTerm(_search.getText());
		});

		_helpButton.setOnAction(e -> {
			cm.show(_helpButton, Side.BOTTOM, 0, 0);
		});

		_contents = new VBox(_searchBar, _message);
		_contents.setPadding(new Insets(15,30,30,30));
		_tab.setContent(_contents);
	}

	/**
	 * Retrieves the wiki search of the supplied term and writes it to a file
	 * @param term	the search term given by user
	 */
	public void searchTerm(String term) {
		_pbSearch.setVisible(true);

		// Term searched using wikit, written to a file and reformatted onto separate lines
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {
				_file = new File ("text.txt");
				SearchManager searchMan = new SearchManager();
				searchMan.searchTerm(_file, term);

				Platform.runLater(new Runnable(){
					// Progress bar is hidden and GUI is updated by reading the text file 
					@Override public void run() {
						try(BufferedReader fileReader = new BufferedReader(new FileReader(_file.toString()))){
							String line = fileReader.readLine();
							// Display contents if there are results, otherwise prompt user to search again
							if(line.contains("not found :^(")) {
								_message.setText("Did you misspell? Try again!");
								_pbSearch.setVisible(false);
								_searchButton.setDisable(false);
								_searchButton.disableProperty().bind(_searchBinding);
								setContents(_main);
							} else {
								_message.setText("");
								_term = term;
								deleteFiles();
								getPics(10, _term); // Download photos for user to select from
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Displays the wiki search results into the TextArea
	 * @param reply	the search term
	 */
	public void displayLines() {
		_pbSearch.setVisible(false);
		_selLines = new SelectLines();
		_selLines.setScreen(_tab, _tabPane, this, _pbCombine, _pbSave, _listLines, _searchBar);
		_searchButton.disableProperty().unbind();
		_searchButton.setDisable(false);
		_searchButton.disableProperty().bind(_searchBinding);
	}

	/**
	 * Displays images for user to choose. It also sets up the GUI for this section,
	 * and lets the user enter a name for the final creation.
	 */
	public void displayImages() {
		VBox chooseImages;
		String potentialName = _term;
		GridPane imgPane = _imMan.getImagePane(_term);

		Label prompt = new Label("Choose pictures you like!");
		prompt.setFont(new Font("Arial", 20));
		prompt.setPadding(new Insets(15,10,10,15));

		TextField nameField = new TextField();
		Button btnCreate = new Button("Create ???");

		int count = 1;
		// Generate new name if already in use
		while (checkName(potentialName).equals(DUPLICATE)) {
			potentialName = _term + "-" + count;
			count++;
		}
		nameField.setText(potentialName);

		// Does not allow characters to be typed into text field
		nameField.textProperty().addListener((observable, oldValue, newValue) -> {
			String[] badCharacters = {"/", "?", "%", "*", ":", "|", "\"", "<", ">", "\0",
					"\\", "(", ")", "$", "@", "!", "#", "^", "&", "+", "="};
			for (String s: badCharacters) {
				if (newValue.contains(s)) {
					nameField.setText(oldValue);
				}
			}
		});
		Pane spacer = new Pane();
		spacer.setMinSize(1, 1);
		HBox nameAndCreate = new HBox(spacer, _pbCombine, nameField, btnCreate);
		HBox.setHgrow(spacer, Priority.ALWAYS);
		nameAndCreate.setPadding(new Insets(10,10,10,10));
		nameAndCreate.setSpacing(10);
		chooseImages = new VBox(prompt, imgPane, nameAndCreate);
		chooseImages.setPadding(new Insets(15,30,30,30));


		ObservableList<File> oToDelete = _imMan.getToDelete();
		// Enter shortcut-key to make creation
		nameField.setOnKeyPressed(arg0 -> {if (arg0.getCode().equals(KeyCode.ENTER)) btnCreate.fire();});
		// Disable create function until photos are selected
		BooleanBinding combBinding = Bindings.size(oToDelete).isEqualTo(10).or(nameField.textProperty().isEmpty());
		btnCreate.disableProperty().bind(combBinding);

		btnCreate.setOnAction(arg0 -> {
			_numberOfPictures = 10 - oToDelete.size();
			for(File imFile: oToDelete){
				imFile.delete();
			}

			//Error checking for creation name
			String name = nameField.getText();
			String validity = checkName(name);
			_name = name;
			if (validity.equals(EMPTY)) {
				nameField.setPromptText("Nothing entered.");
				btnCreate.requestFocus();
			} else if (validity.equals(VALID)) {
				nameField.setPromptText("");
				btnCreate.disableProperty().unbind();
				btnCreate.setDisable(true);
				nameField.setDisable(true);
				combineAudioFiles(); // Site of creation making
			} else if (validity.equals(DUPLICATE)) {
				nameField.clear();
				nameField.setPromptText("");
				btnCreate.requestFocus();
				_popup.showStage(_name, "Name already exists. Overwrite?", "???", "???", false);
			}
		});
		_tab.setContent(chooseImages);
	}

	/**
	 * Checks the validity of the creation name
	 * @param reply	name supplied by user
	 * @return DUPLICATE if the creation already exists
	 * 		   EMPTY if the field is empty
	 * 		   otherwise VALID
	 */
	public String checkName(String reply) {
		File file = new File("./Creations/" + reply + ".mp4");
		if(file.exists()) {
			return DUPLICATE;
		} else if (reply.isEmpty()) {
			return EMPTY;
		} else {
			return VALID;
		}	
	}

	/**
	 * Creates and saves an audio file into the ListView
	 * @param voice	voice selected by user
	 */
	public void addCreation(String voice) {
		if (Home.MODE.getText().equals(Home.ADVANCED)) {
			_pbSave.setVisible(true);
		}
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {

				String cmd = "mkdir -p AudioFiles";
				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);

				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Temporary audio file name
				_numberOfAudioFiles++;
				String nameOfFile = "AudioFile" + _numberOfAudioFiles;


				// Build text-to-speech command based on voice selection
				if (voice.equals(FESTIVAL)) {
					cmd = "cat " + _file.toString() + " | text2wave -o \"./AudioFiles/" + nameOfFile + ".wav\"";
				} else {
					cmd = "espeak -f " + _file.toString() + " --stdout > \"./AudioFiles/" + nameOfFile + ".wav\"";
				}
				builder = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Update list view of audio files
				Platform.runLater(new Runnable(){
					@Override public void run() {
						_view.setContents();
						_listLines.add(nameOfFile);
						_pbSave.setVisible(false);
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Deletes a specified file. Used when the user does not wish to
	 * keep one of their created creations.
	 * @param name name of the file to be deleted
	 */
	public void removeCreation(String name) {
		File file = new File("./Creations/" + name);
		file.delete();
	}

	public void storeTabs(TabPane tabPane) {
		_tabPane = tabPane;
	}

	public void setMusic(String music) {
		_music = music;
	}

	/**
	 * Creates creation by combining audio files, downloading images, creating video, and merging them all into 
	 * one mp4 video file
	 */
	public void combineAudioFiles() {
		_pbCombine.setVisible(true);
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				AudioManager audMan = new AudioManager();
				audMan.combineAudio(_music, _listLines);

				VideoMaker vidMaker = new VideoMaker();
				vidMaker.makeVideo(_term, _name, _numberOfPictures);

				// Create question and add to the set
				Question question = new Question(new File("Quizzes/"+ _term + ".mp4"), _term);
				_set.addQuestion(question);

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						_view.setContents();
						_main.refreshGUI(null);
						_popup.showFeedback(_name, false);
						_pbCombine.setVisible(false);
					}
				});
				deleteFiles();

				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Deletes supporting files from working directory. Cleans up the
	 * directory by removing unnecessary files which are no longer needed
	 * for the application to run.
	 */
	public void deleteFiles() {
		// Reset field values
		_listLines = FXCollections.observableArrayList();
		_numberOfAudioFiles = 0;
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {

				String cmd = "if [ -d AudioFiles ]; then rm -rf AudioFiles; fi; rm -f .*.jpg; ";
				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		new Thread(task).start();
	}

	/**
	 * Downloads a number of images (of the search term) from Flickr
	 * @param input	the number of images
	 * @param reply	the search term
	 */
	public void getPics(int input, String reply) {
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				_imMan.getImages(reply);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						displayLines();
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}
}


