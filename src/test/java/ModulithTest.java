import formbox.FormboxApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithTest {

	@Test
	void verifyModuleBoundariesAndGenerateDocs() {
		var modules = ApplicationModules.of(FormboxApplication.class);

		modules.forEach(System.out::println);

		modules.verify();

		new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
	}
}