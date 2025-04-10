# CoaezUtility - RuneScape 3 Bot Script

## Features

- **Multiple Activities**:
  - Powder of Burials
  - Soil Sifting (Spell)
  - Soil Screening (Mesh)
  - High Level Alchemy
  - Invention Disassembly
  - Gem Crafting

- **Task Management**:
  - Each activity is implemented as a separate task class
  - Centralized task execution through main script loop

- **Configuration**:
  - Persistent configuration saving/loading
  - Customizable item lists for Alchemy and Disassembly
  - Bank preset support for inventory management

- **GUI Interface**:
  - Built with ImGui
  - Tabbed interface for different activities
  - Real-time status display
  - Start/Stop controls

## Getting Started

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/CoaezUtility.git
   ```

2. Build the project using Gradle:
   ```bash
   ./gradlew build
   ```

3. The compiled JAR will be automatically copied to your BotWithUs scripts directory.

### Usage

1. Launch RuneScape 3 and log in to your account
2. Start the BotWithUs client
3. Load the CoaezUtility script
4. Use the GUI to configure and start activities

## Code Structure

The main components of the project are:

- `CoaezUtility.java`: Main script class handling state management and task execution
- `CoaezUtilityGUI.java`: Graphical user interface implementation
- `tasks/`: Package containing individual task implementations
- `model/`: Package containing core functionality models (Alchemy, Disassembly)

## Configuration

The script supports configuration persistence through the `ScriptConfig` system. The following settings are saved:

- Current bot state
- Alchemy item patterns
- Disassembly item patterns

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a new branch for your feature/bugfix
3. Commit your changes
4. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- BotWithUs team for the API and support
- RuneScape community for inspiration and testing
