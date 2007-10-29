/*
 * Created on Oct 29, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

public interface LogFileTailerListener
{
  /**
   * A new line has been added to the tailed log file
   * 
   * @param line   The new line that has been added to the tailed log file
   */
  public void newLogFileLine( String line );
}