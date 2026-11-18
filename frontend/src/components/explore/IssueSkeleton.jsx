import React from 'react';

/**
 * Skeleton placeholder matching the IssueCard geometry during API loading.
 */
export function IssueSkeleton() {
  return (
    <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-4 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="h-4 bg-[#262626] w-1/3" />
        <div className="flex space-x-2">
          <div className="h-4 bg-[#262626] w-16" />
          <div className="h-4 bg-[#262626] w-12" />
        </div>
      </div>

      <div className="space-y-2">
        <div className="h-6 bg-[#2A2A2A] w-3/4" />
        <div className="h-4 bg-[#222222] w-full" />
        <div className="h-4 bg-[#222222] w-5/6" />
      </div>

      <div className="flex gap-2">
        <div className="h-5 bg-[#222222] w-20" />
        <div className="h-5 bg-[#222222] w-24" />
        <div className="h-5 bg-[#222222] w-16" />
      </div>

      <div className="p-3 bg-[#0D0D0D] border border-[#222222] space-y-2">
        <div className="h-3 bg-[#262626] w-1/4" />
        <div className="h-3 bg-[#222222] w-full" />
        <div className="h-3 bg-[#222222] w-2/3" />
      </div>

      <div className="flex items-center justify-between pt-2 border-t border-[#222222]">
        <div className="h-4 bg-[#222222] w-1/4" />
        <div className="h-6 bg-[#262626] w-20" />
      </div>
    </div>
  );
}

export default IssueSkeleton;
