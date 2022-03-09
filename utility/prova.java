package utility;
/**
	 * The switches at the beginning of the path need to install different
	 * rules in their Flow Table. The path that involves the following switches
	 * is returned by this function: if the Path object has this structure
	 * || sw1, outport || sw2, inport || sw2, outport || ...
	 * || sw(n-1), inport || sw(n-1), outport || sw(n), inport ||
	 * then the returned Path is
	 * || sw2, outport || ...
	 * || sw(n-1), inport || sw(n-1), outport || sw(n), inport ||
	 * @param route The Path object whose head has to be cut
	 * @return The cut route
	 */
	private static Path cutRouteHead(Path route) {
		Path newRoute = null;
		List<NodePortTuple> list = route.getPath();
		if (list.size() > 1) {
			Iterator<NodePortTuple> iterator = list.iterator();
			iterator.next();
			NodePortTuple entry = iterator.next();
			newRoute = new Path(entry.getNodeId(), route.getId().getDst());
			ArrayList<NodePortTuple> temp = new ArrayList<>();
			while(iterator.hasNext()) {
				temp.add(iterator.next());
			}
			newRoute.setPath(temp);
		}
		return newRoute;
	}
}
