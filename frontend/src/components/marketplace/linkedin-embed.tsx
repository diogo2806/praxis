import { Linkedin } from "lucide-react";

import { Button } from "@/components/ui/button";

export function LinkedinEmbed({ url }: { url?: string | null }) {
  if (!url) {
    return null;
  }
  return (
    <Button asChild variant="outline">
      <a href={url} target="_blank" rel="noreferrer">
        <Linkedin className="h-4 w-4" />
        LinkedIn
      </a>
    </Button>
  );
}
