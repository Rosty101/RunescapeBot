package AntiAntiBotPack;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;


public class ToolNameAdjuster extends AbstractScript{
	public String Start(String nameToAdj) {
		String returnString = adjustName(nameToAdj);
		return returnString;
	}
	
	private String adjustName(String nameToAdj) {
		switch(nameToAdj) {
		case "axe":
			int skill = getSkills().getRealLevel(Skill.SMITHING);
			if(skill < 15)
			{
				return "Bronze " + nameToAdj;
			}
			else if(skill < 30)
			{
				return "Iron " + nameToAdj;
			}
			return "just here for now";
			default:
				return"";
		}
	}

	@Override
	public int onLoop() {
		// TODO Auto-generated method stub
		return 0;
	}
}
