import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Manager {
    //String url = "jdbc:sqlserver://localhost:1433;encrypt=true;databaseName=EAFC24;trustServerCertificate=true";
    //String username = "sql";
    private String password = "0000";

    public void addManager() throws InputMismatchException, SQLException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(LeagueApp.url, LeagueApp.username, password);
            con.setAutoCommit(false); // Start transaction
            String insertManagerSQL = "INSERT INTO managers (name, origin_id, league_admin) VALUES (?, ?, ?);";
            try (PreparedStatement ps = con.prepareStatement(insertManagerSQL, Statement.RETURN_GENERATED_KEYS)) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter manager's name: ");
                String managerName = scanner.nextLine();
                //String managerName = "Ali";
                System.out.println("Enter manager's EA ID: ");
                String originID = scanner.nextLine();
                //String originID = "Dr_Epico";
                System.out.println("Is manager an admin? (true/false): ");
                boolean leagueAdmin = scanner.nextBoolean();
                scanner.nextLine(); // Consume newline character

                //boolean leagueAdmin = true;

                String checkManagerSQL = "SELECT COUNT(*) FROM managers WHERE origin_id = ?;";
                try (PreparedStatement checkOriginPs = con.prepareStatement(checkManagerSQL)) {
                    checkOriginPs.setString(1, originID);
                    ResultSet originResult = checkOriginPs.executeQuery();
                    originResult.next();
                    int count = originResult.getInt(1);
                    if (count > 0) {
                        System.out.println("Origin ID already exists. Skipping insertion.");
                        return; // Skip insertion
                    }
                }

                ps.setString(1, managerName);
                ps.setString(2, originID);
                ps.setBoolean(3, leagueAdmin);
                ps.executeUpdate();

                // Retrieve generated manager ID
                ResultSet generatedId = ps.getGeneratedKeys();
                if (generatedId.next()) {
                    int newManagerId = generatedId.getInt(1);

                    //Update specified club with newly created manager ID
                    String updateClubSQL = "UPDATE clubs SET manager_id = ? WHERE club_name = ?;"; //LIKE better?
                    try (PreparedStatement updateClubPs = con.prepareStatement(updateClubSQL)) {
                        System.out.println("Enter club name associated with the manager: ");
                        String clubName = scanner.nextLine();//todo: check if club exists or not
                        //String clubName = "Arsenal";

                        String checkClubSQL = "SELECT manager_id FROM clubs WHERE club_name = ? AND manager_id IS NOT NULL;";
                        try (PreparedStatement checkClubPs = con.prepareStatement(checkClubSQL)) {
                            checkClubPs.setString(1, clubName);
                            ResultSet clubResult = checkClubPs.executeQuery();
                            if (clubResult.next()) {
                                System.out.println("Club already has a manager assigned. Please choose a different club.");
                                return; //Skip insertion
                            }
                        }

                        updateClubPs.setInt(1, newManagerId);
                        updateClubPs.setString(2, clubName);
                        updateClubPs.executeUpdate();
                        System.out.println("Manager assigned successfully (Generated manager ID: " + newManagerId + ")");
                    }
                }
                con.commit(); // Commit transaction if everything is successful
                System.out.println("Transaction committed successfully.");
            } catch (SQLException e) {
                con.rollback(); // Rollback transaction if an error occurs
                System.out.println("Transaction rolled back due to error: " + e.getMessage());
                e.printStackTrace();
            }
        } catch(SQLException e){
            System.out.println("Failed to establish database connection: " + e.getMessage());
            e.printStackTrace();
        } finally{
            if (con != null) {
                try {
                    con.setAutoCommit(true); // Reset auto-commit mode
                    con.close();
                } catch (SQLException e) {
                    System.out.println("Failed to close database connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}