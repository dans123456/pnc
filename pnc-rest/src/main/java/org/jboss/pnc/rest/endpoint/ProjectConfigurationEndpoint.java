package org.jboss.pnc.rest.endpoint;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.jboss.pnc.rest.provider.ProjectConfigurationProvider;
import org.jboss.pnc.rest.restmodel.ProjectBuildConfigurationRest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@Api(value = "/product/{productId}/version/{versionId}/project/{projectId}/configuration", description = "Project Configuration related information")
@Path("/product/{productId}/version/{versionId}/project/{projectId}/configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectConfigurationEndpoint {

    private ProjectConfigurationProvider projectConfigurationProvider;

    public ProjectConfigurationEndpoint() {
    }

    @Inject
    public ProjectConfigurationEndpoint(ProjectConfigurationProvider projectConfigurationProvider) {
        this.projectConfigurationProvider = projectConfigurationProvider;
    }

    @ApiOperation(value = "Gets specific all Projects configuration")
    @GET
    public List<ProjectBuildConfigurationRest> getAll(
            @ApiParam(value = "Product id", required = true) @PathParam("productId") Integer productId,
            @ApiParam(value = "Product Version id", required = true) @PathParam("versionId") Integer productVersionId,
            @ApiParam(value = "Project id", required = true) @PathParam("projectId") Integer projectId) {
        return projectConfigurationProvider.getAll(projectId);
    }

    @ApiOperation(value = "Gets specific Project's configuration")
    @GET
    @Path("/{id}")
    public ProjectBuildConfigurationRest getSpecific(
            @ApiParam(value = "Product id", required = true) @PathParam("productId") Integer productId,
            @ApiParam(value = "Product Version id", required = true) @PathParam("versionId") Integer productVersionId,
            @ApiParam(value = "Project id", required = true) @PathParam("projectId") Integer projectId,
            @ApiParam(value = "Project's Configuration id", required = true) @PathParam("id") Integer id) {
        return projectConfigurationProvider.getSpecific(projectId, id);
    }

    @ApiOperation(value = "Creates new Project's configuration")
    @POST
    public Response createNew(
            @ApiParam(value = "Product id", required = true) @PathParam("productId") Integer productId,
            @ApiParam(value = "Product Version id", required = true) @PathParam("versionId") Integer productVersionId,
            @ApiParam(value = "Project id", required = true) @PathParam("projectId") Integer projectId,
            @NotNull @Valid ProjectBuildConfigurationRest projectBuildConfigurationRest,
            @Context UriInfo uriInfo) {
        UriBuilder uriBuilder = UriBuilder.fromUri(uriInfo.getRequestUri()).path("{id}");
        int id = projectConfigurationProvider.store(projectId, projectBuildConfigurationRest);
        return Response.created(uriBuilder.build(uriBuilder.build(id))).build();
    }

    @ApiOperation(value = "Deletes Project's configuration")
    @DELETE
    @Path("/{id}")
    public Response delete(
            @ApiParam(value = "Product id", required = true) @PathParam("productId") Integer productId,
            @ApiParam(value = "Product Version id", required = true) @PathParam("versionId") Integer productVersionId,
            @ApiParam(value = "Project id", required = true) @PathParam("projectId") Integer projectId,
            @ApiParam(value = "Project's Configuration id", required = true) @PathParam("id") Integer id) {
        projectConfigurationProvider.delete(id);
        return Response.ok().build();
    }
}
