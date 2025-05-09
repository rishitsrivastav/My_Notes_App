# NotesApp - Modern Android Notes Application

![NotesApp Banner](https://github.com/username/test/raw/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png)

A sleek, modern Android notes application with rich media support, built with Material Design principles and Room database.

## Features

- **Rich Text Notes**: Create and edit notes with titles and content
- **Media Support**: Add photos and videos to your notes
- **Search Functionality**: Quickly find notes with the built-in search feature
- **Material Design**: Beautiful, intuitive UI following Material Design guidelines
- **Animations**: Smooth transitions and animations throughout the app
- **Dark Mode Support**: Automatically adapts to system dark mode settings
- **Edge-to-Edge Design**: Immersive full-screen experience

## Screenshots

<table>
  <tr>
    <td><img src="screenshots/home_screen.png" alt="Home Screen" width="200"/></td>
    <td><img src="screenshots/add_note.png" alt="Add Note" width="200"/></td>
    <td><img src="screenshots/note_with_media.png" alt="Note with Media" width="200"/></td>
    <td><img src="screenshots/search_notes.png" alt="Search Notes" width="200"/></td>
  </tr>
</table>

## Architecture

The app follows the MVVM (Model-View-ViewModel) architecture pattern and uses the following components:

- **Room Database**: For local data persistence
- **LiveData**: For reactive UI updates
- **RecyclerView**: For efficient list display
- **Material Components**: For modern UI elements
- **Glide**: For image loading and caching
- **ExoPlayer**: For video playback

## Technical Details

### Database Schema

The app uses Room database with the following entities:

- **Note**: Stores note information (id, title, content, timestamp)
- **Media**: Stores media information (id, noteId, type, path, timestamp)

### Key Components

- **MainActivity**: Main entry point, handles note list display and search
- **NoteAdapter**: RecyclerView adapter for displaying notes
- **MediaAdapter**: RecyclerView adapter for displaying media in notes
- **NoteRepository**: Handles data operations for notes
- **MediaRepository**: Handles data operations for media
- **AppDatabase**: Room database configuration

## Getting Started

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK 21 or higher
- Gradle 7.0 or higher

## Permissions

The app requires the following permissions:

- `READ_MEDIA_IMAGES` (Android 13+): For accessing photos
- `READ_MEDIA_VIDEO` (Android 13+): For accessing videos
- `READ_EXTERNAL_STORAGE` (Android 6-12): For accessing media files
- `WRITE_EXTERNAL_STORAGE`: For saving media files

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Material Design guidelines
- Android Jetpack libraries
- Room persistence library
- Glide image loading library
- ExoPlayer video playback library

## Contact

Your Name - [@your_twitter](https://twitter.com/your_twitter) - email@example.com

Project Link: [https://github.com/username/test](https://github.com/username/test)
